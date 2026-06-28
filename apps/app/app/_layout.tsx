import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { colors } from '../theme/styles';
import { ApiHealthProvider } from '../contexts/ApiHealthContext';
import { AnnouncementModal } from '../components/AnnouncementModal';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      <ApiHealthProvider>
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: colors.background },
            animation: 'none',
          }}
        />
        <AnnouncementModal />
      </ApiHealthProvider>
    </SafeAreaProvider>
  );
}
