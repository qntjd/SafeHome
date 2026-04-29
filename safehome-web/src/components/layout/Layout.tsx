import { Outlet, useLocation } from 'react-router-dom'
import Navbar from './Navbar'
import { useState, useCallback } from 'react'
import { useSse } from '@/hooks/useSse'
import AlertToast from '@/components/alert/AlertToast'
import type { AlertEvent } from '@/types'
import SosOverlay from '@/components/SosOverlay'

export default function Layout() {
  const { pathname } = useLocation()
  const [alerts, setAlerts] = useState<AlertEvent[]>([])

  const handleAlert = useCallback((event: AlertEvent) => {
    setAlerts((prev) => [event, ...prev.slice(0, 4)])
    setTimeout(() => setAlerts((prev) => prev.slice(0, -1)), 5000)
  }, [])

  useSse(handleAlert)

  const isMapPage = pathname === '/map' || pathname === '/trip'

  return (
    <div className="flex flex-col h-screen" style={{ background: 'var(--bg-primary)' }}>
      <Navbar />
      <main className={`flex-1 ${isMapPage ? 'overflow-hidden' : 'overflow-y-auto'}`}>
        <Outlet />
      </main>
      <SosOverlay /> 
      <div className="fixed bottom-4 right-4 flex flex-col gap-2 z-50 max-w-xs w-full">
        {alerts.map((alert, i) => (
          <AlertToast key={i} alert={alert} />
        ))}
      </div>
    </div>
  )
}