import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';

/// Selector del número de la tapa (0-99) con ajuste fino y edición directa.
class CapNumberSelector extends StatefulWidget {
  final int? value;
  final ValueChanged<int?> onChanged;
  final bool enabled;

  const CapNumberSelector({
    super.key,
    required this.value,
    required this.onChanged,
    this.enabled = true,
  });

  @override
  State<CapNumberSelector> createState() => _CapNumberSelectorState();
}

class _CapNumberSelectorState extends State<CapNumberSelector> {
  late final TextEditingController _controller;
  final FocusNode _focus = FocusNode();

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.value?.toString() ?? '');
  }

  @override
  void didUpdateWidget(covariant CapNumberSelector oldWidget) {
    super.didUpdateWidget(oldWidget);
    final text = widget.value?.toString() ?? '';
    if (!_focus.hasFocus && _controller.text != text) {
      _controller.text = text;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    _focus.dispose();
    super.dispose();
  }

  void _step(int delta) {
    final current = widget.value ?? (delta > 0 ? -1 : 100);
    final next = (current + delta).clamp(0, 99);
    _controller.text = next.toString();
    widget.onChanged(next);
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _StepButton(
          icon: Icons.remove_rounded,
          tooltip: 'Restar uno',
          onPressed: widget.enabled && (widget.value ?? 1) > 0 ? () => _step(-1) : null,
        ),
        const SizedBox(width: AppSpacing.md),
        Container(
          width: 140,
          height: 140,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: AppColors.surfaceContainerLowest,
            border: Border.all(color: AppColors.primary, width: 3),
            boxShadow: [BoxShadow(color: AppColors.primary.withValues(alpha: 0.25), blurRadius: 24)],
          ),
          child: TextField(
            controller: _controller,
            focusNode: _focus,
            enabled: widget.enabled,
            textAlign: TextAlign.center,
            keyboardType: TextInputType.number,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(2),
            ],
            style: AppTypography.capDisplay.copyWith(color: AppColors.primary, fontSize: 56),
            cursorColor: AppColors.primary,
            decoration: InputDecoration(
              border: InputBorder.none,
              enabledBorder: InputBorder.none,
              focusedBorder: InputBorder.none,
              disabledBorder: InputBorder.none,
              filled: false,
              hintText: '--',
              hintStyle: AppTypography.capDisplay.copyWith(color: AppColors.outline, fontSize: 56),
            ),
            onChanged: (text) => widget.onChanged(text.isEmpty ? null : int.tryParse(text)),
          ),
        ),
        const SizedBox(width: AppSpacing.md),
        _StepButton(
          icon: Icons.add_rounded,
          tooltip: 'Sumar uno',
          onPressed: widget.enabled && (widget.value ?? -1) < 99 ? () => _step(1) : null,
        ),
      ],
    );
  }
}

class _StepButton extends StatelessWidget {
  final IconData icon;
  final String tooltip;
  final VoidCallback? onPressed;

  const _StepButton({required this.icon, required this.tooltip, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return IconButton.filled(
      onPressed: onPressed,
      tooltip: tooltip,
      icon: Icon(icon),
      iconSize: 24,
      style: IconButton.styleFrom(
        backgroundColor: AppColors.surfaceContainerHigh,
        foregroundColor: AppColors.textPrimary,
        disabledBackgroundColor: AppColors.surfaceContainerLow,
        disabledForegroundColor: AppColors.outline,
        minimumSize: const Size.square(AppSpacing.iconButtonSize),
      ),
    );
  }
}
