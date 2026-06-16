#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
准备ECDICT词库文件供Android assets使用

功能：
1. 读取完整ECDICT CSV
2. 按词频排序
3. 提取前N个常用词
4. 输出到Android assets目录

使用：
    python3 prepare_ecdict_for_assets.py ecdict.csv
"""

import csv
import sys
import os


def prepare_ecdict_for_assets(input_csv, output_path=None, limit=50000):
    """
    准备ECDICT文件供Android使用

    Args:
        input_csv: 输入的ECDICT CSV文件
        output_path: 输出路径（默认: app/src/main/assets/ecdict.csv）
        limit: 词条数量限制
    """

    # 默认输出路径
    if output_path is None:
        output_path = "app/src/main/assets/ecdict.csv"

    print("=" * 60)
    print("ECDICT Assets准备工具")
    print("=" * 60)
    print(f"输入文件: {input_csv}")
    print(f"输出文件: {output_path}")
    print(f"词条限制: {limit:,}")
    print()

    # 检查输入文件
    if not os.path.exists(input_csv):
        print(f"❌ 错误：文件不存在 {input_csv}")
        return False

    # 创建输出目录
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    # 读取和排序
    print("📖 读取CSV文件...")
    words_data = []

    with open(input_csv, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        header = reader.fieldnames

        for row in reader:
            # 计算词频
            try:
                bnc = int(row.get('bnc', 0) or 0)
                frq = int(row.get('frq', 0) or 0)
                frequency = bnc + frq
            except:
                frequency = 0

            # 保存行和词频
            words_data.append((frequency, row))

    print(f"✅ 读取完成，共{len(words_data):,}个词条")
    print()

    # 按词频排序
    print("🔄 按词频排序...")
    words_data.sort(key=lambda x: x[0], reverse=True)
    print("✅ 排序完成")
    print()

    # 限制数量
    words_data = words_data[:limit]
    print(f"📊 保留前{len(words_data):,}个高频词")
    print()

    # 写入输出文件
    print("💾 写入输出文件...")
    with open(output_path, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=header)
        writer.writeheader()

        for i, (freq, row) in enumerate(words_data):
            writer.writerow(row)
            if (i + 1) % 10000 == 0:
                print(f"   进度: {i+1:,}/{len(words_data):,}", end='\r')

        print(f"   进度: {len(words_data):,}/{len(words_data):,}")

    print()

    # 输出文件信息
    file_size_mb = os.path.getsize(output_path) / (1024 * 1024)

    print("=" * 60)
    print("✅ 准备完成！")
    print("=" * 60)
    print(f"📄 输出文件: {os.path.abspath(output_path)}")
    print(f"📊 文件大小: {file_size_mb:.1f} MB")
    print(f"📈 词条数量: {len(words_data):,}")
    print()
    print("🚀 下一步:")
    print("   1. 编译并安装应用")
    print("   2. 首次启动会自动导入词库")
    print("=" * 60)

    return True


def main():
    if len(sys.argv) < 2:
        print("用法: python3 prepare_ecdict_for_assets.py <ecdict.csv> [output_path] [limit]")
        print()
        print("参数:")
        print("  ecdict.csv   - ECDICT CSV文件路径")
        print("  output_path  - 输出路径 (默认: app/src/main/assets/ecdict.csv)")
        print("  limit        - 词条数量 (默认: 50000)")
        print()
        print("示例:")
        print("  python3 prepare_ecdict_for_assets.py ecdict.csv")
        print("  python3 prepare_ecdict_for_assets.py ecdict.csv app/src/main/assets/dict.csv 100000")
        sys.exit(1)

    input_csv = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else None
    limit = int(sys.argv[3]) if len(sys.argv) > 3 else 50000

    success = prepare_ecdict_for_assets(input_csv, output_path, limit)
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
