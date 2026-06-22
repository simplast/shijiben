import 'package:drift/drift.dart';

/// 事件表定义。
///
/// 记录用户日程事件，包含标题、起止时间、状态、关联标签与备注。
/// status 字段：0=notStarted, 1=inProgress, 2=completed
@DataClassName('Event')
class Events extends Table {
  IntColumn get id => integer().autoIncrement()();

  TextColumn get title => text().withLength(min: 1)();

  DateTimeColumn get startTime => dateTime()();

  DateTimeColumn get endTime => dateTime().nullable()();

  IntColumn get status => integer().withDefault(const Constant(0))();

  IntColumn get tagId => integer().nullable()();

  TextColumn get note => text().nullable()();

  DateTimeColumn get createdAt => dateTime()();

  DateTimeColumn get updatedAt => dateTime()();
}
