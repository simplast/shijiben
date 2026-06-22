import 'package:flutter_test/flutter_test.dart';

import 'package:shijiben/main.dart';

void main() {
  testWidgets('App renders preview page', (WidgetTester tester) async {
    await tester.pumpWidget(const ShijibenApp());

    // 验证标题存在
    expect(find.text('事记本'), findsOneWidget);
  });
}
