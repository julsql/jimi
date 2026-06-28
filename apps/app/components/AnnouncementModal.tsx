import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import type { AnnouncementLevel } from '../api/announcement';
import { useAnnouncement } from '../hooks/useAnnouncement';
import { colors, radius, shadow, spacing, typography } from '../theme/styles';

// Per-criticality accent: neutral brand orange → amber → red.
const LEVEL_STYLES: Record<AnnouncementLevel, { color: string; glyph: string; title: string }> = {
  INFO: { color: colors.accent, glyph: 'i', title: 'Information' },
  WARNING: { color: colors.checking, glyph: '!', title: 'Heads up' },
  CRITICAL: { color: colors.offline, glyph: '!', title: 'Important' },
};

// Self-driving popup: it fetches the latest announcement itself and renders
// nothing until there is one to show. Drop a single <AnnouncementModal /> near
// the app root.
export function AnnouncementModal() {
  const { announcement, dismissForNow, dismissForever } = useAnnouncement();

  const visible = announcement !== null;
  const level = announcement ? LEVEL_STYLES[announcement.level] : LEVEL_STYLES.INFO;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={dismissForNow}
    >
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <View style={styles.header}>
            <View style={[styles.iconCircle, { backgroundColor: level.color }]}>
              <Text style={styles.iconGlyph}>{level.glyph}</Text>
            </View>
            <Text style={[styles.title, { color: level.color }]}>{level.title}</Text>
          </View>

          <Text style={styles.message}>{announcement?.message ?? ''}</Text>

          <View style={styles.actions}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Don't show again"
              onPress={dismissForever}
              style={({ pressed }) => [styles.secondary, pressed && styles.secondaryPressed]}
            >
              <Text style={styles.secondaryLabel}>Don&apos;t show again</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="OK"
              onPress={dismissForNow}
              style={({ pressed }) => [
                styles.primary,
                { backgroundColor: level.color },
                pressed && styles.primaryPressed,
              ]}
            >
              <Text style={styles.primaryLabel}>OK</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing.xl,
  },
  card: {
    width: '100%',
    maxWidth: 420,
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    padding: spacing.xl,
    gap: spacing.lg,
    ...shadow.lg,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  iconCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconGlyph: {
    fontFamily: typography.brandFamily,
    color: '#FFFFFF',
    fontSize: 22,
    fontWeight: '700',
    lineHeight: 24,
  },
  title: {
    fontFamily: typography.bodyFamily,
    fontSize: typography.title,
    fontWeight: '700',
  },
  message: {
    fontFamily: typography.bodyFamily,
    fontSize: typography.body,
    color: colors.text,
    lineHeight: 22,
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    gap: spacing.sm,
    marginTop: spacing.xs,
  },
  secondary: {
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: radius.pill,
  },
  secondaryPressed: { backgroundColor: colors.surfaceMuted },
  secondaryLabel: {
    fontFamily: typography.bodyFamily,
    fontSize: typography.caption,
    fontWeight: '600',
    color: colors.textMuted,
  },
  primary: {
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.xl,
    borderRadius: radius.pill,
  },
  primaryPressed: { opacity: 0.85 },
  primaryLabel: {
    fontFamily: typography.bodyFamily,
    fontSize: typography.body,
    fontWeight: '700',
    color: '#FFFFFF',
  },
});
