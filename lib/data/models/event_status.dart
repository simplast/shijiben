/// 事件状态枚举。
///
/// 0 = 未开始，1 = 进行中，2 = 已完成。
/// 数据库中以 int 存储，通过 [value] 与 [fromValue] 互转。
enum EventStatus {
  notStarted,
  inProgress,
  completed;

  int get value => index;

  static EventStatus fromValue(int value) => EventStatus.values[value];
}
