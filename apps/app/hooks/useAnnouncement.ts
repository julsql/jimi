import { useCallback, useEffect, useState } from 'react';
import { fetchLatestAnnouncement, type Announcement } from '../api/announcement';
import { addDismissedId, isAnnouncementDismissed, loadDismissedIds } from '../api/announcementDismissals';

interface AnnouncementState {
  // The announcement to show in the popup, or null when there is nothing to show.
  announcement: Announcement | null;
  // "OK": close for this session — it may reappear next launch.
  dismissForNow: () => void;
  // "Don't show again": persist the id so this exact message never shows again.
  dismissForever: () => void;
}

// Fetches the latest announcement at startup and decides whether to show it
// (hidden if the user already tapped "Don't show again" on that id). Failures
// are swallowed by the API/storage layers, so this never blocks the app.
export function useAnnouncement(): AnnouncementState {
  const [announcement, setAnnouncement] = useState<Announcement | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const [latest, dismissed] = await Promise.all([
        fetchLatestAnnouncement(),
        loadDismissedIds(),
      ]);
      if (cancelled || !latest) return;
      if (isAnnouncementDismissed(latest.id, dismissed)) return;
      setAnnouncement(latest);
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const dismissForNow = useCallback(() => {
    setAnnouncement(null);
  }, []);

  const dismissForever = useCallback(() => {
    setAnnouncement((current) => {
      if (current) void addDismissedId(current.id);
      return null;
    });
  }, []);

  return { announcement, dismissForNow, dismissForever };
}
