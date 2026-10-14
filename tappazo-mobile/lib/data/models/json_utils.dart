/// Utilidades de parseo tolerante para payloads del backend.
library;

/// `Instant` de Jackson: ISO-8601 (por defecto en Spring Boot) o epoch
/// numérico en segundos con fracción (si WRITE_DATES_AS_TIMESTAMPS está activo).
DateTime? parseInstant(dynamic value) {
  if (value == null) return null;
  if (value is String) return DateTime.tryParse(value);
  if (value is num) {
    return DateTime.fromMicrosecondsSinceEpoch((value * 1000000).round(), isUtc: true);
  }
  return null;
}

List<String> stringList(dynamic value) {
  if (value is List) {
    return value.whereType<String>().toList(growable: false);
  }
  return const [];
}

/// El backend usa `""` en lugar de `null` en varios payloads (Map.of no admite null).
String? nonEmptyString(dynamic value) {
  if (value is String && value.isNotEmpty) return value;
  return null;
}
