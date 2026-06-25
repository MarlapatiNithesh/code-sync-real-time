import { useSettings } from "@/context/SettingContext"
import { useEffect } from "react"

function usePageEvents() {
    const { fontSize, setFontSize } = useSettings()



    useEffect(() => {
        const handleWheel = (e: any) => {
            if (e.ctrlKey) {
                // Prevent default browser zoom behavior
                e.preventDefault()
                if (!e.target.closest(".cm-editor")) return
                if (e.deltaY > 0) {
                    setFontSize(Math.max(fontSize - 1, 12))
                } else {
                    setFontSize(Math.min(fontSize + 1, 24))
                }
            }
        }

        window.addEventListener("wheel", handleWheel, { passive: false })

        return () => {
            window.removeEventListener("wheel", handleWheel)
        }
    }, [fontSize, setFontSize])
}

export default usePageEvents
