import 'package:drift/drift.dart';

/// 随笔表定义。
///
/// 记录用户在某时刻写下的简短随笔内容。
@DataClassName('Note')
class Notes extends Table {
  IntColumn get id => integer().autoIncrement()();

  TextColumn get content => text()();

  DateTimeColumn get timestamp => dateTime()();

  DateTimeColumn get createdAt => dateTime()();

  DateTimeColumn get updatedAt => dateTime()();
}
