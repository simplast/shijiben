import 'package:drift/drift.dart';

import '../database/app_database.dart';
import '../tables/tags_table.dart';

part 'tag_dao.g.dart';

/// 标签数据访问对象。
///
/// 提供标签的增删改查，按 sortOrder 排序。
@DriftAccessor(tables: [Tags])
class TagDao extends DatabaseAccessor<AppDatabase> with _$TagDaoMixin {
  TagDao(super.db);

  /// 查询全部标签，按 sortOrder 升序返回。
  Stream<List<Tag>> getAllTags() {
    return (select(tags)..orderBy([(t) => OrderingTerm.asc(t.sortOrder)]))
        .watch();
  }

  /// 插入一条新标签，返回新记录 id。
  Future<int> insertTag({
    required String name,
    required int color,
    int sortOrder = 0,
  }) {
    final now = DateTime.now();
    return into(tags).insert(TagsCompanion.insert(
      name: name,
      color: color,
      sortOrder: Value(sortOrder),
      createdAt: now,
      updatedAt: now,
    ));
  }

  /// 更新一条标签（整行替换）。
  Future<bool> updateTag(Tag tag) {
    return update(tags).replace(tag);
  }

  /// 删除指定 id 的标签，返回受影响行数。
  Future<int> deleteTag(int id) {
    return (delete(tags)..where((t) => t.id.equals(id))).go();
  }
}
