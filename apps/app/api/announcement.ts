import { ApiConstants } from './constants';

// Broadcast announcement shown in a popup at app startup. Controlled entirely
// server-side (GET /announcement) so we can warn users about an outage,
// maintenance, a new feature… without shipping a new app build.

export type AnnouncementLevel = 'INFO' | 'WARNING' | 'CRITICAL';

export interface Announcement {
  id: number;
  message: string;
  level: AnnouncementLevel;
  createdAt: string | null;
}

const LEVELS: readonly AnnouncementLevel[] = ['INFO', 'WARNING', 'CRITICAL'];

function parseLevel(value: unknown): AnnouncementLevel {
  return typeof value === 'string' && (LEVELS as readonly string[]).includes(value)
    ? (value as AnnouncementLevel)
    : 'INFO';
}

// Returns the latest active announcement, or null when there is none (HTTP 204)
// or the API is unreachable — the popup must never block app startup.
export async function fetchLatestAnnouncement(): Promise<Announcement | null> {
  try {
    const res = await fetch(`${ApiConstants.baseUrl}${ApiConstants.announcement}`, {
      headers: { Accept: 'application/json' },
    });
    if (res.status === 204 || !res.ok) return null;
    const json = (await res.json()) as {
      id?: unknown;
      message?: unknown;
      level?: unknown;
      createdAt?: unknown;
    };
    if (typeof json.id !== 'number' || typeof json.message !== 'string') return null;
    return {
      id: json.id,
      message: json.message,
      level: parseLevel(json.level),
      createdAt: typeof json.createdAt === 'string' ? json.createdAt : null,
    };
  } catch {
    return null;
  }
}
