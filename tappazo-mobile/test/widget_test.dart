import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/main.dart';

void main() {
  testWidgets('Tappazo smoke test renders splash screen', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: TappazoApp(),
      ),
    );

    expect(find.text('TAPPAZO'), findsOneWidget);
    expect(find.text('¿Quién paga la ronda?'), findsOneWidget);
  });
}
