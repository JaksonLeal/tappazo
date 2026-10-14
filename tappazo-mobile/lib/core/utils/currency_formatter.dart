import 'package:intl/intl.dart';

class CurrencyFormatter {
  CurrencyFormatter._();

  static final NumberFormat _formatter = NumberFormat.currency(
    locale: 'es_CO',
    symbol: '\$',
    decimalDigits: 0,
  );

  /// Formatea un monto entero al estándar de Tappazo: "$4.000 COP"
  static String format(num amount) {
    return '${_formatter.format(amount)} COP';
  }
}

