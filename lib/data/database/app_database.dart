import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

import '../daos/event_dao.dart';
import '../daos/note_dao.dart';
import '../daos/tag_dao.dart';
import '../tables/events_table.dart';
import '../tables/notes_table.dart';
import '../tables/tags_table.dart';

part 'app_database.g.dart';

/// 事记本本地数据库。
///
/// 包含三张表：[Events]（事件）、[Notes]（随笔）、[Tags]（标签）。
/// 通过 DAO 访问数据：[EventDao]、[NoteDao]、[TagDao]。
@DriftDatabase(
  tables: [Events, Notes, Tags],
  daos: [EventDao, NoteDao, TagDao],
)
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  /// 用于测试的构造函数，可传入自定义的 [QueryExecutor]。
  AppDatabase.forTesting(super.e);

  @override
  int get schemaVersion => 1;
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'shijiben.sqlite'));
    return NativeDatabase.createInBackground(file);
  });
}
