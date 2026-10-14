import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../domain/entities/game_decisions.dart';
import '../../providers/game_session_providers.dart';
import '../../providers/game_session_state.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import '../../widgets/game/cap_number_selector.dart';
import '../../widgets/game/game_card.dart';
import '../../widgets/tappazo_button.dart';

/// Turno de revelación (Secciones 19-21, 26):
/// foto obligatoria → sugerencia del asistente local → confirmación/corrección
/// manual (0-99) → subida prefirmada → envío del RevealAttempt por STOMP.
class RevealCaptureScreen extends ConsumerStatefulWidget {
  final String gameId;
  final int? nextAttemptNumber;

  const RevealCaptureScreen({super.key, required this.gameId, this.nextAttemptNumber});

  @override
  ConsumerState<RevealCaptureScreen> createState() => _RevealCaptureScreenState();
}

class _RevealCaptureScreenState extends ConsumerState<RevealCaptureScreen> {
  final ImagePicker _picker = ImagePicker();

  Uint8List? _photoBytes;
  int? _number;
  bool _numberSuggested = false;
  bool _analyzing = false;
  bool _capturing = false;
  PhotoOmittedReason? _cameraFailure;
  bool _omitPhoto = false;

  @override
  void initState() {
    super.initState();
    // Abrimos la cámara directamente: la foto es obligatoria por defecto.
    WidgetsBinding.instance.addPostFrameCallback((_) => _takePhoto());
  }

  Future<void> _takePhoto() async {
    if (_capturing) return;
    setState(() => _capturing = true);
    try {
      final file = await _picker.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: CameraDevice.rear,
        imageQuality: 80,
        maxWidth: 1600,
      );
      if (file == null || !mounted) return; // Cancelado por el usuario.
      final bytes = await file.readAsBytes();
      if (!mounted) return;
      setState(() {
        _photoBytes = bytes;
        _cameraFailure = null;
        _omitPhoto = false;
        _numberSuggested = false;
      });
      await _runAssistant(file.path);
    } on PlatformException catch (e) {
      if (!mounted) return;
      setState(() {
        _cameraFailure = switch (e.code) {
          'camera_access_denied' || 'camera_access_restricted' => PhotoOmittedReason.cameraPermissionDenied,
          'no_available_camera' => PhotoOmittedReason.cameraUnavailable,
          _ => PhotoOmittedReason.cameraError,
        };
      });
    } catch (_) {
      if (mounted) setState(() => _cameraFailure = PhotoOmittedReason.cameraError);
    } finally {
      if (mounted) setState(() => _capturing = false);
    }
  }

  Future<void> _runAssistant(String path) async {
    final recognizer = ref.read(capNumberRecognizerProvider);
    if (!recognizer.isAvailable) return;
    setState(() => _analyzing = true);
    try {
      final suggestion = await recognizer.suggestNumber(path);
      if (!mounted) return;
      if (suggestion != null && suggestion >= 0 && suggestion <= 99) {
        setState(() {
          _number = suggestion;
          _numberSuggested = true;
        });
      }
    } catch (_) {
      // El asistente es opcional: si falla, el jugador ingresa el número.
    } finally {
      if (mounted) setState(() => _analyzing = false);
    }
  }

  bool get _hasEvidence => _photoBytes != null || (_omitPhoto && _cameraFailure != null);

  Future<void> _submit() async {
    final number = _number;
    if (number == null || !_hasEvidence) return;
    FocusScope.of(context).unfocus();
    final ok = await ref.read(gameSessionProvider(widget.gameId).notifier).submitAttempt(
          proposedNumber: number,
          photoBytes: _photoBytes,
          photoOmittedReason: _photoBytes == null ? _cameraFailure : null,
        );
    if (ok && mounted) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(gameSessionProvider(widget.gameId));
    final submission = session.submission;
    final busy = submission.stage == SubmissionStage.uploading || submission.stage == SubmissionStage.sending;
    final recognizerAvailable = ref.watch(capNumberRecognizerProvider).isAvailable;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(
          widget.nextAttemptNumber != null && widget.nextAttemptNumber! > 1
              ? 'REVELAR · INTENTO ${widget.nextAttemptNumber}'
              : 'REVELAR TAPA',
          style: AppTypography.headlineSm,
        ),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.margin, vertical: AppSpacing.md),
                children: [
                  _buildPhotoArea(),
                  if (_cameraFailure != null) ...[
                    const SizedBox(height: AppSpacing.md),
                    _buildCameraFailureCard(),
                  ],
                  const SizedBox(height: AppSpacing.xl),
                  Text('NÚMERO DE LA TAPA', style: AppTypography.labelSm, textAlign: TextAlign.center),
                  const SizedBox(height: AppSpacing.md),
                  CapNumberSelector(
                    value: _number,
                    enabled: !busy,
                    onChanged: (n) => setState(() {
                      _number = n;
                      _numberSuggested = false;
                    }),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  _buildAssistantHint(recognizerAvailable),
                ],
              ),
            ),
            _buildBottomBar(busy, submission),
          ],
        ),
      ),
    );
  }

  Widget _buildPhotoArea() {
    final bytes = _photoBytes;
    if (bytes == null) {
      return GameCard(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.xl, horizontal: AppSpacing.md),
        child: Column(
          children: [
            Icon(
              _omitPhoto ? Icons.no_photography_outlined : Icons.photo_camera_outlined,
              size: 48,
              color: _omitPhoto ? AppColors.outline : AppColors.primary,
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              _omitPhoto ? 'Intento sin foto' : 'Fotografía tu tapa',
              style: AppTypography.headlineSm,
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              _omitPhoto
                  ? 'Quedará marcado como excepción por falla de cámara.'
                  : 'La foto es obligatoria para que la mesa valide tu número.',
              textAlign: TextAlign.center,
              style: AppTypography.bodySm,
            ),
            const SizedBox(height: AppSpacing.md),
            TappazoButton(
              text: _capturing ? 'Abriendo cámara…' : 'Tomar foto',
              icon: Icons.photo_camera_rounded,
              isLoading: _capturing,
              onPressed: _takePhoto,
            ),
          ],
        ),
      );
    }

    return Stack(
      children: [
        ClipRRect(
          borderRadius: AppSpacing.borderRadiusMd,
          child: AspectRatio(
            aspectRatio: 4 / 3,
            child: Image.memory(bytes, fit: BoxFit.cover),
          ),
        ),
        Positioned(
          right: AppSpacing.sm,
          top: AppSpacing.sm,
          child: FilledButton.tonalIcon(
            onPressed: _capturing ? null : _takePhoto,
            icon: const Icon(Icons.refresh_rounded, size: 18),
            label: const Text('Repetir'),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.surfaceContainerHighest.withValues(alpha: 0.9),
              foregroundColor: AppColors.textPrimary,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildCameraFailureCard() {
    final reason = _cameraFailure!;
    return GameCard(
      color: AppColors.errorContainer.withValues(alpha: 0.25),
      borderColor: AppColors.error.withValues(alpha: 0.5),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.warning_amber_rounded, color: AppColors.error),
              const SizedBox(width: AppSpacing.sm),
              Expanded(child: Text(reason.description, style: AppTypography.labelMd)),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            'Solo si la cámara falla puedes enviar el número sin foto. '
            'El intento quedará marcado para la mesa.',
            style: AppTypography.bodySm,
          ),
          const SizedBox(height: AppSpacing.xs),
          SwitchListTile.adaptive(
            contentPadding: EdgeInsets.zero,
            value: _omitPhoto,
            activeColor: AppColors.primary,
            title: Text('Continuar sin foto', style: AppTypography.labelMd),
            onChanged: (v) => setState(() => _omitPhoto = v),
          ),
        ],
      ),
    );
  }

  Widget _buildAssistantHint(bool recognizerAvailable) {
    final (icon, text, color) = _analyzing
        ? (Icons.auto_awesome_rounded, 'Analizando la foto…', AppColors.primary)
        : _numberSuggested
            ? (Icons.auto_awesome_rounded, 'Número sugerido por el asistente. Corrígelo si no coincide.', AppColors.secondary)
            : recognizerAvailable
                ? (Icons.edit_rounded, 'Confirma o corrige el número (0-99).', AppColors.outline)
                : (Icons.edit_rounded, 'Ingresa el número que ves en la tapa (0-99).', AppColors.outline);
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(icon, size: 16, color: color),
        const SizedBox(width: AppSpacing.sm),
        Flexible(child: Text(text, textAlign: TextAlign.center, style: AppTypography.bodySm.copyWith(color: color))),
      ],
    );
  }

  Widget _buildBottomBar(bool busy, RevealSubmission submission) {
    final ready = _number != null && _hasEvidence;
    final label = switch (submission.stage) {
      SubmissionStage.uploading => 'Subiendo foto…',
      SubmissionStage.sending => 'Enviando intento…',
      _ => _number == null ? 'Elige el número' : 'Enviar ${_number.toString().padLeft(2, '0')}',
    };
    return Container(
      padding: const EdgeInsets.fromLTRB(AppSpacing.margin, AppSpacing.md, AppSpacing.margin, AppSpacing.md),
      decoration: const BoxDecoration(
        color: AppColors.surfaceContainerLow,
        border: Border(top: BorderSide(color: AppColors.surfaceContainerHighest)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (submission.stage == SubmissionStage.failed && submission.error != null) ...[
            Text(submission.error!, style: AppTypography.bodySm.copyWith(color: AppColors.error)),
            const SizedBox(height: AppSpacing.sm),
          ],
          if (!ready && !busy)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.sm),
              child: Text(
                !_hasEvidence ? 'Falta la foto de la tapa.' : 'Falta el número.',
                style: AppTypography.bodySm,
              ),
            ),
          TappazoButton(
            text: label,
            icon: busy ? null : Icons.send_rounded,
            isLoading: busy,
            onPressed: ready && !busy ? _submit : null,
          ),
        ],
      ),
    );
  }
}
