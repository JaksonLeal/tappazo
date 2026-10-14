import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../providers/game_session_providers.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';

/// Foto de una tapa almacenada en el bucket (fileKey → URL prefirmada).
class RevealPhoto extends ConsumerWidget {
  final String? fileKey;
  final double height;

  const RevealPhoto({super.key, required this.fileKey, this.height = 200});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final key = fileKey;
    if (key == null || key.isEmpty) {
      return _placeholder(Icons.no_photography_outlined, 'Sin foto (falla de cámara)');
    }
    final url = ref.watch(revealPhotoUrlProvider(key));
    return ClipRRect(
      borderRadius: AppSpacing.borderRadiusMd,
      child: SizedBox(
        height: height,
        width: double.infinity,
        child: url.when(
          loading: _loading,
          error: (_, __) => _placeholder(Icons.broken_image_outlined, 'No se pudo cargar la foto'),
          data: (u) => Image.network(
            u,
            fit: BoxFit.cover,
            loadingBuilder: (context, child, progress) => progress == null ? child : _loading(),
            errorBuilder: (_, __, ___) => _placeholder(Icons.broken_image_outlined, 'No se pudo cargar la foto'),
          ),
        ),
      ),
    );
  }

  Widget _loading() => Container(
        height: height,
        color: AppColors.surfaceContainerLowest,
        alignment: Alignment.center,
        child: const SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.primary),
        ),
      );

  Widget _placeholder(IconData icon, String text) => Container(
        height: height,
        width: double.infinity,
        decoration: BoxDecoration(
          color: AppColors.surfaceContainerLowest,
          borderRadius: AppSpacing.borderRadiusMd,
        ),
        alignment: Alignment.center,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: AppColors.outline, size: 32),
            const SizedBox(height: AppSpacing.xs),
            Text(text, style: AppTypography.bodySm),
          ],
        ),
      );
}
