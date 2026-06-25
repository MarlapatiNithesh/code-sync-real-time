import { useAppContext } from "@/context/AppContext"
import { useSocket } from "@/context/SocketContext"
import useWindowDimensions from "@/hooks/useWindowDimensions"
import { SocketEvent } from "@/types/socket"
import { useCallback, useEffect, useState } from "react"
import { HistoryEntry, RecordsDiff, TLRecord, Tldraw, useEditor } from "tldraw"

function DrawingEditor() {
    const { isMobile } = useWindowDimensions()

    return (
        <Tldraw
            inferDarkMode
            forceMobile={isMobile}
            defaultName="Editor"
            className="z-0"
        >
            <ReachEditor />
        </Tldraw>
    )
}

function ReachEditor() {
    const editor = useEditor()
    const { drawingData, setDrawingData } = useAppContext()
    const { socket } = useSocket()
    const [hasLoaded, setHasLoaded] = useState(false)

    const handleChangeEvent = useCallback(
        (change: HistoryEntry<TLRecord>) => {
            const snapshot = change.changes
            // Update the drawing data in the context
            setDrawingData(editor.store.getSnapshot())
            // Emit the snapshot to the server
            socket.emit(SocketEvent.DRAWING_UPDATE, { snapshot })
        },
        [editor.store, setDrawingData, socket],
    )
    //handle drawing updates from other clients
    const handleRemoteDrawing = useCallback(
        ({ snapshot }: { snapshot: RecordsDiff<TLRecord> }) => {
            editor.store.mergeRemoteChanges(() => {
                const { added, updated, removed } = snapshot

                for (const record of Object.values(added)) {
                    editor.store.put([record])
                }
                for (const [, to] of Object.values(updated)) {
                    editor.store.put([to])
                }
                for (const record of Object.values(removed)) {
                    editor.store.remove([record.id])
                }
            })

            setDrawingData(editor.store.getSnapshot())
        },
        [editor.store, setDrawingData],
    )

    // Save active page ID whenever the page changes
    useEffect(() => {
        const checkPageChange = () => {
            const pageId = editor.getCurrentPageId()
            if (pageId) {
                sessionStorage.setItem("activePageId", pageId)
            }
        }

        const cleanup = editor.store.listen(() => {
            checkPageChange()
        })

        return () => cleanup()
    }, [editor])

    useEffect(() => {
        if (hasLoaded) return
        // Load the drawing data from the context
        if (
            drawingData &&
            typeof drawingData === "object" &&
            "store" in drawingData &&
            "schema" in drawingData
        ) {
            editor.store.loadSnapshot(drawingData)

            // Restore active page ID if saved and exists
            const savedPageId = sessionStorage.getItem("activePageId")
            if (savedPageId && editor.store.get(savedPageId as any)) {
                editor.setCurrentPage(savedPageId as any)
            }

            setHasLoaded(true)
        }
    }, [drawingData, hasLoaded, editor])

    useEffect(() => {
        const cleanupFunction = editor.store.listen(handleChangeEvent, {
            source: "user",
            scope: "document",
        })
        // Listen for drawing updates from other clients
        socket.on(SocketEvent.DRAWING_UPDATE, handleRemoteDrawing)

        // Cleanup
        return () => {
            cleanupFunction()
            socket.off(SocketEvent.DRAWING_UPDATE)
        }
    }, [
        editor.store,
        handleChangeEvent,
        handleRemoteDrawing,
        socket,
    ])

    return null
}

export default DrawingEditor
