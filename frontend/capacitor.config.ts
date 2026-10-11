import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'app.studygalaxy',
  appName: 'StudyGalaxy',
  webDir: 'dist',
  backgroundColor: '#05020D',
  android: {
    allowMixedContent: false,
    captureInput: true,
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1200,
      backgroundColor: '#05020D',
      androidSplashResourceName: 'splash',
      androidScaleType: 'CENTER_CROP',
      showSpinner: false,
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#05020D',
    },
  },
  server: {
    androidScheme: 'https',
  },
};

export default config;
