import { useEffect, useRef } from 'react'
import * as THREE from 'three'

interface LighthouseProps {
  size?: number
  fill?: boolean
  className?: string
}


export default function Lighthouse({ size = 240, fill = false, className }: LighthouseProps) {
  const mountRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const mount = mountRef.current
    if (!mount) return

    const prefersReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    mount.appendChild(renderer.domElement)

    const scene = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(42, 1, 0.1, 100)
    camera.position.set(0, 0.2, 5.2)
    camera.lookAt(0, 0, 0)

    const applySize = (w: number, h: number) => {
      if (w <= 0 || h <= 0) return
      renderer.setSize(w, h)
      camera.aspect = w / h
      camera.updateProjectionMatrix()
    }

    let ro: ResizeObserver | null = null
    if (fill) {
      applySize(mount.clientWidth, mount.clientHeight)
      ro = new ResizeObserver(() => applySize(mount.clientWidth, mount.clientHeight))
      ro.observe(mount)
    } else {
      applySize(size, size)
    }


    scene.add(new THREE.AmbientLight(0x8899cc, 0.55))
    const keyLight = new THREE.PointLight(0xaac4ff, 1.1, 20)
    keyLight.position.set(3, 4, 5)
    scene.add(keyLight)
    const beaconLight = new THREE.PointLight(0xffb347, 1.6, 6)
    beaconLight.position.set(0, 1.55, 1.5)
    scene.add(beaconLight)

    // ---------- Lighthouse point cloud (same technique as a dot globe) ----------
    const darkColor = new THREE.Color(0x0c1230)
    const creamColor = new THREE.Color(0xeef1fb)
    const amberColor = new THREE.Color(0xffb347)
    const lampColor = new THREE.Color(0xfff2d6)

    const lhPositions: number[] = []
    const lhColors: number[] = []

    const addFrustumPoints = ({
      count,
      rBottom,
      rTop,
      height,
      centerY,
      color,
    }: {
      count: number
      rBottom: number
      rTop: number
      height: number
      centerY: number
      color: THREE.Color | ((t: number, y: number) => THREE.Color)
    }) => {
      for (let i = 0; i < count; i++) {
        const t = Math.random() // 0 = bottom, 1 = top
        const radius = rBottom + (rTop - rBottom) * t
        const angle = Math.random() * Math.PI * 2
        const y = centerY - height / 2 + t * height
        const jitter = 1 + (Math.random() - 0.5) * 0.06
        const x = Math.cos(angle) * radius * jitter
        const z = Math.sin(angle) * radius * jitter
        lhPositions.push(x, y, z)

        const c = typeof color === 'function' ? color(t, y) : color
        lhColors.push(c.r, c.g, c.b)
      }
    }

    // 받침대
    addFrustumPoints({ count: 260, rBottom: 0.78, rTop: 0.62, height: 0.42, centerY: -1.35, color: darkColor })

    // 몸통 (탑) — 특정 높이 구간은 앰버 줄무늬
    addFrustumPoints({
      count: 950,
      rBottom: 0.46,
      rTop: 0.34,
      height: 2.0,
      centerY: -0.15,
      color: (_t: number, y: number) => {
        const inLowerStripe = y > -0.66 && y < -0.44
        const inUpperStripe = y > 0.04 && y < 0.26
        return inLowerStripe || inUpperStripe ? amberColor : creamColor
      },
    })

    // 램프룸
    addFrustumPoints({ count: 300, rBottom: 0.36, rTop: 0.36, height: 0.42, centerY: 1.05, color: darkColor })

    // 램프 유리
    addFrustumPoints({ count: 240, rBottom: 0.26, rTop: 0.26, height: 0.34, centerY: 1.05, color: lampColor })

    // 지붕
    addFrustumPoints({ count: 260, rBottom: 0.42, rTop: 0.0, height: 0.4, centerY: 1.45, color: darkColor })

    const lhGeo = new THREE.BufferGeometry()
    lhGeo.setAttribute('position', new THREE.Float32BufferAttribute(lhPositions, 3))
    lhGeo.setAttribute('color', new THREE.Float32BufferAttribute(lhColors, 3))
    const lhMat = new THREE.PointsMaterial({
      size: 0.055,
      vertexColors: true,
      transparent: true,
      opacity: 0.95,
      sizeAttenuation: true,
    })
    const lighthouse = new THREE.Points(lhGeo, lhMat)
    scene.add(lighthouse)

    // ---------- Rotating beam ----------
    const beamPivot = new THREE.Group()
    beamPivot.position.set(0, 1.05, 0)
    scene.add(beamPivot)

    const beamGeo = new THREE.ConeGeometry(2.4, 5.5, 28, 1, true)
    beamGeo.rotateX(Math.PI / 2)
    beamGeo.translate(0, 0, 2.9)
    const beamMat = new THREE.MeshBasicMaterial({
      color: 0xffcf8a,
      transparent: true,
      opacity: 0.16,
      blending: THREE.AdditiveBlending,
      side: THREE.DoubleSide,
      depthWrite: false,
    })
    const beam = new THREE.Mesh(beamGeo, beamMat)
    beamPivot.add(beam)

    // ---------- Animation loop ----------
    let rafId = 0
    const clock = new THREE.Clock()
    const animate = () => {
      const dt = clock.getDelta()
      if (!prefersReduced) {
        lighthouse.rotation.y += dt * 0.15
        beamPivot.rotation.y += dt * 0.9
      }
      renderer.render(scene, camera)
      rafId = requestAnimationFrame(animate)
    }
    animate()

    // ---------- Cleanup ----------
    return () => {
      cancelAnimationFrame(rafId)
      ro?.disconnect()
      mount.removeChild(renderer.domElement)
      lhGeo.dispose()
      beamGeo.dispose()
      lhMat.dispose()
      beamMat.dispose()
      renderer.dispose()
    }
  }, [size, fill])

  return (
    <div
      ref={mountRef}
      className={className}
      style={fill ? { width: '100%', height: '100%' } : { width: size, height: size }}
    />
  )
}