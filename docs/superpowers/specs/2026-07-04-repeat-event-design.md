# 已完成事件「再来一次」设计文档

## 背景
首页时间轴中，已完成事件（status == Completed）目前只有展示作用。用户希望给这类事件一个快捷入口，点一下就能复制一条相同内容的新事件并立即开始计时。

## 目标
- 为已完成事件提供一键「再来一次」按钮。
- 点击后复制原事件标题/备注，以当前真实时间开始计时。
- 不修改、不删除原事件。

## 方案
采用 **ViewModel 驱动** 方案（方案 A），改动集中、与现有开始/停止/删除逻辑保持一致。

## 详细设计

### UI：`EventCard`（`feature/timeline/TimelineScreen.kt`）
- 当前左侧只在 `event.status != 2` 时显示开始/停止按钮。
- 调整后：
  - `status == NotStarted(0)`：显示 ▶ 播放按钮，点击 `onStart()`。
  - `status == InProgress(1)`：显示 ⏹ 方形停止按钮，点击 `onStop()`。
  - `status == Completed(2)`：显示 ⟳ 循环箭头（`Icons.Default.Refresh`）按钮，contentDescription = "再来一次"，点击 `onRepeatEvent()`。
- 图标尺寸保持 32dp，图标大小 20dp，与现有开始按钮一致。

### 事件传递
- `EventCard` 新增参数 `onRepeatEvent: () -> Unit`。
- `EventList` 新增参数 `onRepeatEvent: (EventEntity) -> Unit`，并向下传递。
- `TimelineScreen` 在调用 `EventList` 处传入：`onRepeatEvent = { event -> viewModel.repeatEvent(event.id) }`。

### ViewModel：`TimelineViewModel`
新增方法：

```kotlin
/** 再来一次：复制已完成事件并立即开始计时 */
fun repeatEvent(eventId: Long) {
    viewModelScope.launch {
        val event = eventRepository.getEventById(eventId) ?: return@launch
        val now = System.currentTimeMillis()
        eventRepository.createEvent(
            title = event.title,
            startTime = now,
            endTime = null,
            note = event.note,
            status = EventStatus.InProgress.value
        )
        refresh()
    }
}
```

### 边界行为
- 原事件不存在：直接返回，无操作。
- 原事件未完成：按钮不会出现，因此不会触发。
- 当前已有进行中事件：本次不主动停止它，与首页现有「开始」按钮行为保持一致。
- 新事件 startTime 取 `System.currentTimeMillis()`，即真实当前时间；即使正在回看过去日期也会出现在今天。

## 测试
在 `TimelineViewModelTest` 新增一条测试：
- Given：一条已完成事件。
- When：调用 `viewModel.repeatEvent(event.id)`。
- Then：
  - 数据库中存在原 Completed 事件（id 不变）。
  - 数据库中新增一条 InProgress 事件，title/note 与原事件相同，startTime 为调用时刻。

## 涉及文件
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`
- `app/src/test/java/com/shijiben/feature/timeline/TimelineViewModelTest.kt`
