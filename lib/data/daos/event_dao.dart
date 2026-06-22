import 'package:drift/drift.dart';

import '../database/app_database.dart';
import '../models/event_status.dart';
import '../tables/events_table.dart';

part 'event_dao.g.dart';

/// 事件数据访问对象。
///
/// 提供事件的增删改查以及状态流转（开始/完成/结转）。
@DriftAccessor(tables: [Events])
class EventDao extends DatabaseAccessor<AppDatabase> with _$EventDaoMixin {
  EventDao(super.db);

  /// 查询某天的事件（startTime 在 [date] 当天范围内），按 startTime 升序返回。
  Stream<List<Event>> getEventsByDate(DateTime date) {
    final start = DateTime(date.year, date.month, date.day);
    final end = start.add(const Duration(days: 1));
    return (select(events)
          ..where((e) => e.startTime.isBiggerOrEqualValue(start) & e.startTime.isSmallerThanValue(end))
          ..orderBy([(e) => OrderingTerm.asc(e.startTime)]))
        .watch();
  }

  /// 查询当前进行中的事件（status = inProgress）。
  Stream<List<Event>> getOngoingEvent() {
    return (select(events)
          ..where((e) => e.status.equals(EventStatus.inProgress.value))
          ..limit(1))
        .watch();
  }

  /// 查询 startTime 早于 [date] 且状态为 notStarted 的事件。
  Future<List<Event>> getNotStartedBeforeDate(DateTime date) {
    return (select(events)
          ..where((e) => e.startTime.isSmallerThanValue(date) & e.status.equals(EventStatus.notStarted.value)))
        .get();
  }

  /// 插入一条新事件，返回新记录 id。
  Future<int> insertEvent({
    required String title,
    required DateTime startTime,
    DateTime? endTime,
    EventStatus status = EventStatus.notStarted,
    int? tagId,
    String? note,
  }) {
    final now = DateTime.now();
    return into(events).insert(EventsCompanion.insert(
      title: title,
      startTime: startTime,
      endTime: Value(endTime),
      status: Value(status.value),
      tagId: Value(tagId),
      note: Value(note),
      createdAt: now,
      updatedAt: now,
    ));
  }

  /// 更新一条事件（整行替换）。
  Future<bool> updateEvent(Event event) {
    return update(events).replace(event);
  }

  /// 删除指定 id 的事件，返回受影响行数。
  Future<int> deleteEvent(int id) {
    return (delete(events)..where((e) => e.id.equals(id))).go();
  }

  /// 将事件置为进行中，并清空 endTime。
  Future<int> startEvent(int id) {
    final now = DateTime.now();
    return (update(events)..where((e) => e.id.equals(id))).write(EventsCompanion(
      status: Value(EventStatus.inProgress.value),
      endTime: const Value(null),
      updatedAt: Value(now),
    ));
  }

  /// 将事件置为已完成，并写入 endTime。
  Future<int> completeEvent(int id, DateTime endTime) {
    final now = DateTime.now();
    return (update(events)..where((e) => e.id.equals(id))).write(EventsCompanion(
      status: Value(EventStatus.completed.value),
      endTime: Value(endTime),
      updatedAt: Value(now),
    ));
  }

  /// 将之前日期的 notStarted 事件结转到今天，保留相同时刻。
  ///
  /// 例如原定 06-20 14:00 的事件，今天为 06-22，则改为 06-22 14:00。
  Future<void> carryOverNotStarted() {
    final now = DateTime.now();
    final startOfToday = DateTime(now.year, now.month, now.day);
    return transaction(() async {
      final pending = await (select(events)
            ..where((e) =>
                e.startTime.isSmallerThanValue(startOfToday) &
                e.status.equals(EventStatus.notStarted.value)))
          .get();
      for (final event in pending) {
        final newStart = DateTime(
          now.year,
          now.month,
          now.day,
          event.startTime.hour,
          event.startTime.minute,
          event.startTime.second,
        );
        await (update(events)..where((e) => e.id.equals(event.id))).write(
          EventsCompanion(
            startTime: Value(newStart),
            updatedAt: Value(now),
          ),
        );
      }
    });
  }
}
