import 'package:drift/drift.dart';

import '../database/app_database.dart';
import '../tables/notes_table.dart';

part 'note_dao.g.dart';

/// 随笔数据访问对象。
///
/// 提供随笔的增删改查。
@DriftAccessor(tables: [Notes])
class NoteDao extends DatabaseAccessor<AppDatabase> with _$NoteDaoMixin {
  NoteDao(super.db);

  /// 查询某天的随笔（timestamp 在 [date] 当天范围内），按时间升序返回。
  Stream<List<Note>> getNotesByDate(DateTime date) {
    final start = DateTime(date.year, date.month, date.day);
    final end = start.add(const Duration(days: 1));
    return (select(notes)
          ..where((n) => n.timestamp.isBiggerOrEqualValue(start) & n.timestamp.isSmallerThanValue(end))
          ..orderBy([(n) => OrderingTerm.asc(n.timestamp)]))
        .watch();
  }

  /// 插入一条新随笔，返回新记录 id。
  Future<int> insertNote({
    required String content,
    required DateTime timestamp,
  }) {
    final now = DateTime.now();
    return into(notes).insert(NotesCompanion.insert(
      content: content,
      timestamp: timestamp,
      createdAt: now,
      updatedAt: now,
    ));
  }

  /// 更新一条随笔（整行替换）。
  Future<bool> updateNote(Note note) {
    return update(notes).replace(note);
  }

  /// 删除指定 id 的随笔，返回受影响行数。
  Future<int> deleteNote(int id) {
    return (delete(notes)..where((n) => n.id.equals(id))).go();
  }
}
