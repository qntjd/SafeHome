import { useState, useEffect } from 'react'

export type PermissionStatus = 'idle' | 'granted' | 'denied' | 'requesting'

export function usePermissions() {
  const [locationStatus, setLocationStatus] = useState<PermissionStatus>('idle')
  const [notificationStatus, setNotificationStatus] = useState<PermissionStatus>('idle')

  useEffect(() => {
    // 이미 허용/거부 상태 확인
    navigator.permissions?.query({ name: 'geolocation' }).then((result) => {
      if (result.state === 'granted') setLocationStatus('granted')
      if (result.state === 'denied')  setLocationStatus('denied')
    })

    if ('Notification' in window) {
      if (Notification.permission === 'granted') setNotificationStatus('granted')
      if (Notification.permission === 'denied')  setNotificationStatus('denied')
    }
  }, [])

  const requestLocation = (): Promise<boolean> => {
    return new Promise((resolve) => {
      setLocationStatus('requesting')
      navigator.geolocation.getCurrentPosition(
        () => { setLocationStatus('granted'); resolve(true) },
        () => { setLocationStatus('denied');  resolve(false) }
      )
    })
  }

  const requestNotification = async (): Promise<boolean> => {
    if (!('Notification' in window)) return false
    setNotificationStatus('requesting')
    const result = await Notification.requestPermission()
    if (result === 'granted') { setNotificationStatus('granted'); return true }
    setNotificationStatus('denied')
    return false
  }

  return { locationStatus, notificationStatus, requestLocation, requestNotification }
}