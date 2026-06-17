#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ECDICT词库处理脚本

功能：
1. 读取ECDICT CSV文件
2. 按词频排序筛选常用词
3. 生成SQLite数据库供Android使用

使用：
    python3 process_ecdict.py ecdict.csv words.db 50000
"""

import csv
import sqlite3
import os
import sys
import re
import time


def clean_text(text):
    """清理文本"""
    if not text:
        return ''
    # 移除多余空格
    text = re.sub(r'\s+', ' ', text.strip())
    return text


def determine_level(tag, oxford, collins):
    """
    根据标签确定单词等级

    优先级：CET4 > CET6 > 考研 > TOEFL > IELTS > GRE > OXFORD > COMMON
    """
    if not tag:
        if oxford and oxford.strip():
            return 'OXFORD'
        if collins:
            try:
                col_level = int(collins)
                if col_level >= 4:
                    return 'COMMON'
            except:
                pass
        return None

    tag_lower = tag.lower()

    if 'cet4' in tag_lower:
        return 'CET4'
    elif 'cet6' in tag_lower:
        return 'CET6'
    elif '考研' in tag or 'kaoyan' in tag_lower:
        return 'KAOYAN'
    elif 'toefl' in tag_lower or '托福' in tag:
        return 'TOEFL'
    elif 'ielts' in tag_lower or '雅思' in tag:
        return 'IELTS'
    elif 'gre' in tag_lower:
        return 'GRE'
    elif 'sat' in tag_lower:
        return 'SAT'
    else:
        return 'COMMON'


def should_include_word(word, translation):
    """
    判断是否应该包含该单词

    过滤规则：
    - 单词长度不超过50
    - 必须有中文翻译
    - 不包含特殊字符（保留连字符和撇号）
    """
    if not word or not translation:
        return False

    if len(word) > 50:
        return False

    # 只允许字母、连字符和撇号
    if not re.match(r"^[a-zA-Z][a-zA-Z\-']*$", word):
        return False

    return True


def process_ecdict(csv_path, output_db_path, limit=50000):
    """
    处理ECDICT CSV文件，生成SQLite数据库

    Args:
        csv_path: ECDICT CSV文件路径
        output_db_path: 输出数据库路径
        limit: 导入词条数量限制（按词频排序）
    """

    print("=" * 60)
    print("ECDICT词库处理工具")
    print("=" * 60)
    print(f"输入文件: {csv_path}")
    print(f"输出文件: {output_db_path}")
    print(f"词条限制: {limit:,}")
    print()

    # 检查输入文件
    if not os.path.exists(csv_path):
        print(f"❌ 错误：文件不存在 {csv_path}")
        return False

    file_size_mb = os.path.getsize(csv_path) / (1024 * 1024)
    print(f"📄 文件大小: {file_size_mb:.1f} MB")
    print()

    # 创建数据库连接
    print("🔧 创建数据库...")
    if os.path.exists(output_db_path):
        os.remove(output_db_path)

    conn = sqlite3.connect(output_db_path)
    cursor = conn.cursor()

    cursor.execute('PRAGMA user_version=2')

    # 创建表（与当前 Room schema 保持一致）
    cursor.executescript('''
        CREATE TABLE words (
            word TEXT NOT NULL,
            phonetic TEXT,
            phoneticUs TEXT,
            phoneticUk TEXT,
            definition TEXT NOT NULL,
            translation TEXT NOT NULL,
            level TEXT,
            frequency INTEGER NOT NULL,
            exchange TEXT,
            collins INTEGER,
            bnc INTEGER,
            frq INTEGER,
            tag TEXT,
            addedTime INTEGER NOT NULL,
            pos TEXT,
            oxford INTEGER,
            detail TEXT,
            audio TEXT,
            PRIMARY KEY(word)
        );
        CREATE INDEX index_words_word ON words(word);

        CREATE TABLE user_words (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            word TEXT NOT NULL,
            addedDate INTEGER NOT NULL,
            lastReviewDate INTEGER,
            nextReviewDate INTEGER NOT NULL,
            easeFactor REAL NOT NULL,
            interval INTEGER NOT NULL,
            repetitions INTEGER NOT NULL,
            status TEXT NOT NULL,
            knownCount INTEGER NOT NULL,
            unknownCount INTEGER NOT NULL,
            testCorrectCount INTEGER NOT NULL,
            testTotalCount INTEGER NOT NULL,
            isFavorite INTEGER NOT NULL,
            notes TEXT
        );
        CREATE INDEX index_user_words_word ON user_words(word);
        CREATE INDEX index_user_words_nextReviewDate ON user_words(nextReviewDate);
        CREATE INDEX index_user_words_status ON user_words(status);

        CREATE TABLE examples (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            word TEXT NOT NULL,
            sentenceEn TEXT NOT NULL,
            sentenceZh TEXT NOT NULL,
            source TEXT,
            audioUrl TEXT,
            FOREIGN KEY(word) REFERENCES words(word) ON UPDATE NO ACTION ON DELETE CASCADE
        );
        CREATE INDEX index_examples_word ON examples(word);

        CREATE TABLE study_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            date INTEGER NOT NULL,
            wordsLearned INTEGER NOT NULL,
            wordsReviewed INTEGER NOT NULL,
            testCorrect INTEGER NOT NULL,
            testTotal INTEGER NOT NULL,
            studyDuration INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
        );
        CREATE UNIQUE INDEX index_study_records_date ON study_records(date);
    ''')

    conn.commit()
    print("✅ 数据库表创建完成")
    print()

    # 读取CSV
    print("📖 读取CSV文件...")
    words_data = []
    total_read = 0
    filtered_count = 0

    try:
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)

            for row in reader:
                total_read += 1

                if total_read % 10000 == 0:
                    print(f"   已读取: {total_read:,} 条", end='\r')

                # 提取字段
                word = clean_text(row.get('word', ''))
                translation = clean_text(row.get('translation', ''))

                # 过滤
                if not should_include_word(word, translation):
                    filtered_count += 1
                    continue

                phonetic = clean_text(row.get('phonetic', ''))
                definition = clean_text(row.get('definition', ''))
                tag = row.get('tag', '')
                oxford = row.get('oxford', '')
                collins = row.get('collins', '')

                # 词频（BNC + frq）
                try:
                    bnc = int(row.get('bnc', 0) or 0)
                    frq = int(row.get('frq', 0) or 0)
                    frequency = bnc + frq
                except:
                    frequency = 0

                # 确定等级
                level = determine_level(tag, oxford, collins)

                words_data.append({
                    'word': word,
                    'phonetic': phonetic or None,
                    'definition': definition,
                    'translation': translation,
                    'level': level,
                    'frequency': frequency,
                    'addedTime': int(time.time() * 1000)
                })

        print(f"   已读取: {total_read:,} 条")
        print()
        print(f"✅ CSV读取完成")
        print(f"   总词条: {total_read:,}")
        print(f"   有效词条: {len(words_data):,}")
        print(f"   过滤词条: {filtered_count:,}")
        print()

    except Exception as e:
        print(f"❌ 读取CSV失败: {e}")
        conn.close()
        return False

    # 按词频排序
    print("🔄 按词频排序...")
    words_data.sort(key=lambda x: x['frequency'] if x['frequency'] > 0 else float('inf'))
    print("✅ 排序完成")
    print()

    # 限制数量
    original_count = len(words_data)
    words_data = words_data[:limit]
    print(f"📊 筛选结果:")
    print(f"   原始数量: {original_count:,}")
    print(f"   保留数量: {len(words_data):,}")
    print()

    # 统计等级分布
    level_stats = {}
    for word in words_data:
        level = word['level'] or 'NONE'
        level_stats[level] = level_stats.get(level, 0) + 1

    print("📈 等级分布:")
    for level, count in sorted(level_stats.items(), key=lambda x: x[1], reverse=True):
        percentage = count / len(words_data) * 100
        print(f"   {level:10s}: {count:6,} ({percentage:5.1f}%)")
    print()

    # 批量插入
    print("💾 导入数据库...")
    batch_size = 1000
    total = len(words_data)

    try:
        for i in range(0, total, batch_size):
            batch = words_data[i:i+batch_size]
            cursor.executemany('''
                INSERT OR IGNORE INTO words (
                    word, phonetic, definition, translation, level, frequency, addedTime
                ) VALUES (
                    :word, :phonetic, :definition, :translation, :level, :frequency, :addedTime
                )
            ''', batch)

            conn.commit()
            progress = min(i+batch_size, total)
            percentage = progress / total * 100
            print(f"   进度: {progress:,}/{total:,} ({percentage:.1f}%)", end='\r')

        print(f"   进度: {total:,}/{total:,} (100.0%)")
        print()

    except Exception as e:
        print(f"❌ 导入失败: {e}")
        conn.close()
        return False

    # 验证
    cursor.execute('SELECT COUNT(*) FROM words')
    count = cursor.fetchone()[0]

    cursor.execute('SELECT word FROM words ORDER BY CASE WHEN frequency > 0 THEN 0 ELSE 1 END, frequency ASC LIMIT 10')
    top_words = [row[0] for row in cursor.fetchall()]

    conn.close()

    # 输出结果
    print("=" * 60)
    print("✅ 导入完成！")
    print("=" * 60)
    print(f"📊 统计信息:")
    print(f"   导入词条: {count:,}")
    print(f"   数据库大小: {os.path.getsize(output_db_path) / (1024*1024):.1f} MB")
    print(f"   输出路径: {os.path.abspath(output_db_path)}")
    print()
    print(f"🔝 高频词Top 10:")
    for i, word in enumerate(top_words, 1):
        print(f"   {i:2d}. {word}")
    print()
    print("=" * 60)

    return True


def main():
    """主函数"""
    if len(sys.argv) < 2:
        print("用法: python3 process_ecdict.py <ecdict.csv> [output.db] [limit]")
        print()
        print("参数:")
        print("  ecdict.csv  - ECDICT CSV文件路径")
        print("  output.db   - 输出数据库路径 (默认: words.db)")
        print("  limit       - 导入词条数量 (默认: 50000)")
        print()
        print("示例:")
        print("  python3 process_ecdict.py ecdict.csv")
        print("  python3 process_ecdict.py ecdict.csv words.db 100000")
        sys.exit(1)

    csv_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else 'words.db'
    limit = int(sys.argv[3]) if len(sys.argv) > 3 else 50000

    success = process_ecdict(csv_path, output_path, limit)
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
