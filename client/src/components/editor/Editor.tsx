import { useAppContext } from "@/context/AppContext"
import { useFileSystem } from "@/context/FileContext"
import { useSettings } from "@/context/SettingContext"
import { useSocket } from "@/context/SocketContext"
import usePageEvents from "@/hooks/usePageEvents"
import useResponsive from "@/hooks/useResponsive"
import { editorThemes } from "@/resources/Themes"
import { FileSystemItem } from "@/types/file"
import { SocketEvent } from "@/types/socket"
import { color } from "@uiw/codemirror-extensions-color"
import { hyperLink } from "@uiw/codemirror-extensions-hyper-link"
import { LanguageName, loadLanguage } from "@uiw/codemirror-extensions-langs"
import CodeMirror, {
    Extension,
    ViewUpdate,
    scrollPastEnd,
} from "@uiw/react-codemirror"
import { useEffect, useMemo, useState } from "react"
import toast from "react-hot-toast"
import { cursorTooltipBaseTheme, tooltipField } from "./tooltip"

function Editor() {
    const { users, currentUser } = useAppContext()
    const { activeFile, setActiveFile, isLineEditable, updateFileContent } = useFileSystem()
    const { theme, language, fontSize } = useSettings()
    const { socket } = useSocket()
    const { viewHeight } = useResponsive()
    const [timeOut, setTimeOut] = useState(setTimeout(() => { }, 0))
    const filteredUsers = useMemo(
        () => users.filter((u) => u.username !== currentUser.username),
        [users, currentUser],
    )
    const [extensions, setExtensions] = useState<Extension[]>([])
    const [currentLine, setCurrentLine] = useState<number | null>(null)

    const onCodeChange = (code: string, view: ViewUpdate) => {
        if (!activeFile) return
        const pos = view.state?.selection?.main?.head || 0
        const line = view.state?.doc?.lineAt(pos)?.number || 1

        if (!isLineEditable(activeFile.id, line)) {
            toast.error("This line is locked by another user")
            return
        }

        const file: FileSystemItem = { ...activeFile, content: code }
        setActiveFile(file)
        updateFileContent(activeFile.id, code)
        const cursorPosition = view.state?.selection?.main?.head
        socket.emit(SocketEvent.TYPING_START, { cursorPosition })
        socket.emit(SocketEvent.FILE_UPDATED, {
            fileId: activeFile.id,
            newContent: code,
            lineNumber: line,
        })
        clearTimeout(timeOut)

        const newTimeOut = setTimeout(
            () => socket.emit(SocketEvent.TYPING_PAUSE),
            1000,
        )
        setTimeOut(newTimeOut)
    }

    const onUpdate = (viewUpdate: ViewUpdate) => {
        if (!activeFile) return
        if (viewUpdate.selectionSet) {
            const pos = viewUpdate.state.selection.main.head
            const line = viewUpdate.state.doc.lineAt(pos).number
            if (line !== currentLine) {
                // Release previous line lock if any
                if (currentLine !== null) {
                    socket.emit(SocketEvent.FILE_LOCK_RELEASE, {
                        fileId: activeFile.id,
                        lineNumber: currentLine,
                    })
                }
                // Acquire lock on the new line
                socket.emit(SocketEvent.FILE_LOCK_REQUEST, {
                    fileId: activeFile.id,
                    lineNumber: line,
                })
                setCurrentLine(line)
            }
        }
    }

    // Clean up locks when file changes or component unmounts
    useEffect(() => {
        return () => {
            if (activeFile && currentLine !== null) {
                socket.emit(SocketEvent.FILE_LOCK_RELEASE, {
                    fileId: activeFile.id,
                    lineNumber: currentLine,
                })
            }
        }
    }, [activeFile?.id, currentLine, socket])

    useEffect(() => {
        const handleSave = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key === "s") {
                e.preventDefault()
                toast.success("Workspace synced and saved!")
            }
        }
        window.addEventListener("keydown", handleSave)
        return () => window.removeEventListener("keydown", handleSave)
    }, [])

    usePageEvents()

    useEffect(() => {
        const extensions = [
            color,
            hyperLink,
            tooltipField(filteredUsers),
            cursorTooltipBaseTheme,
            scrollPastEnd(),
        ]
        const langExt = loadLanguage(language.toLowerCase() as LanguageName)
        if (langExt) {
            extensions.push(langExt)
        } else {
            toast.error(
                "Syntax highlighting is unavailable for this language. Please adjust the editor settings; it may be listed under a different name.",
                {
                    duration: 5000,
                },
            )
        }

        setExtensions(extensions)
    }, [filteredUsers, language])

    return (
        <CodeMirror
            theme={editorThemes[theme]}
            onChange={onCodeChange}
            onUpdate={onUpdate}
            value={activeFile?.content}
            editable={true}
            extensions={extensions}
            minHeight="100%"
            maxWidth="100vw"
            style={{
                fontSize: fontSize + "px",
                height: viewHeight,
                position: "relative",
            }}
        />
    )
}

export default Editor
