import AsyncStorage from '@react-native-async-storage/async-storage';

// Persisted set of announcement ids the user dismissed with "Don't show again".
// Stored client-side (not on the server) so there is no per-user backend state:
// a new announcement has a new id, so it is never in this set and reappears.
const STORAGE_KEY = 'jimi.dismissedAnnouncements';

// Pure check, kept separate from storage so it is trivial to reason about/test.
export function isAnnouncementDismissed(id: number, dismissedIds: readonly number[]): boolean {
  return dismissedIds.includes(id);
}

export async function loadDismissedIds(): Promise<number[]> {
  try {
    const raw = await AsyncStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((x): x is number => typeof x === 'number') : [];
  } catch {
    return [];
  }
}

// Adds an id to the dismissed set (idempotent). Best-effort: a storage failure
// just means the popup may show again next launch, never a crash.
export async function addDismissedId(id: number): Promise<void> {
  try {
    const current = await loadDismissedIds();
    if (current.includes(id)) return;
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify([...current, id]));
  } catch {
    // ignore — non-persisted dismissal is acceptable, never block the user.
  }
}
