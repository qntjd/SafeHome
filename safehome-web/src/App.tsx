import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import Layout from '@/components/layout/Layout'
import ProtectedRoute from '@/components/layout/ProtectedRoute'
import LoginPage from '@/pages/LoginPage'
import RegisterPage from '@/pages/RegisterPage'
import DashboardPage from '@/pages/DashboardPage'
import MapPage from '@/pages/MapPage'
import TripPage from '@/pages/TripPage'
import AlertPage from '@/pages/AlertPage'
import NewsPage from '@/pages/NewsPage'
import OAuthCallbackPage from '@/pages/OAuthCallbackPage'
import SettingsPage from '@/pages/SettingsPage'
import CrimeStatsPage from '@/pages/CrimeStatsPage'
import SafetyResourcesPage from './pages/SafetyResourcesPage'
import ShareLocationPage from './pages/ShareLocationPage'

const queryClient = new QueryClient()

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login"    element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
          <Route path="/share/:shareToken" element={<ShareLocationPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/"      element={<DashboardPage />} />
              <Route path="/map"   element={<MapPage />} />
              <Route path="/trip"  element={<TripPage />} />
              <Route path="/alert" element={<AlertPage />} />
              <Route path="/news"  element={<NewsPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/crime" element={<CrimeStatsPage />} />
              <Route path="/resources" element={<SafetyResourcesPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}