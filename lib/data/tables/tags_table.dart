import 'package:drift/drift.dart';

/// 标签表定义。
///
/// 用于对事件进行分类，包含名称、颜色与排序。
@DataClassName('Tag')
class Tags extends Table {
  IntColumn get id => integer().autoIncrement()();

  TextColumn get name => text().withLength(min: 1)();

  IntColumn get color => integer()();

  IntColumn get sortOrder => integer().withDefault(const Constant(0))();

  DateTimeColumn get createdAt => dateTime()();

  DateTimeColumn get updatedAt => dateTime()();
}
