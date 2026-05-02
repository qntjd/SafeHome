export const getFacilityMarkerImage = (type: string) => {
  const colors: Record<string, string> = {
    CCTV:           '#4f7ef8',
    EMERGENCY_BELL: '#f87171',
    STREETLIGHT:    '#fbbf24',
    POLICE:         '#a78bfa',
  }
  const color = colors[type] ?? '#888888'
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36"><path d="M14 0C6.268 0 0 6.268 0 14c0 9.333 14 22 14 22S28 23.333 28 14C28 6.268 21.732 0 14 0z" fill="${color}" stroke="white" stroke-width="2"/><circle cx="14" cy="14" r="6" fill="white"/></svg>`
  return {
    src:  'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svg))),
    size: { width: 28, height: 36 },
  }
}