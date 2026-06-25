import { useCopilot } from "@/context/CopilotContext"
import { useFileSystem } from "@/context/FileContext"
import { useSocket } from "@/context/SocketContext"
import useResponsive from "@/hooks/useResponsive"
import { SocketEvent } from "@/types/socket"
import toast from "react-hot-toast"
import { LuClipboardPaste, LuCopy, LuRepeat } from "react-icons/lu"
import ReactMarkdown from "react-markdown"
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter"
import { dracula } from "react-syntax-highlighter/dist/esm/styles/prism"
import { useState } from "react"
import { Modal } from "../../common/Modal"

function CopilotView() {
    const {socket} = useSocket()
    const { viewHeight } = useResponsive()
    const { generateCode, output, isRunning, setInput } = useCopilot()
    const { activeFile, updateFileContent, setActiveFile } = useFileSystem()
    const [isReplaceConfirmOpen, setIsReplaceConfirmOpen] = useState(false)

    const copyOutput = async () => {
        const content = output.replace(/```[\w]*\n?/g, "").trim()
        if (navigator.clipboard && window.isSecureContext) {
            try {
                await navigator.clipboard.writeText(content)
                toast.success("Output copied to clipboard")
                return
            } catch (error) {
                console.error("Secure clipboard copy failed, using fallback...", error)
            }
        }

        // Fallback
        const textarea = document.createElement("textarea")
        textarea.value = content
        textarea.style.position = "fixed"
        textarea.style.opacity = "0"
        document.body.appendChild(textarea)
        textarea.focus()
        textarea.select()
        try {
            document.execCommand("copy")
            toast.success("Output copied to clipboard")
        } catch (err) {
            toast.error("Unable to copy output to clipboard")
            console.error("Fallback copy failed:", err)
        }
        document.body.removeChild(textarea)
    }


    const pasteCodeInFile = () => {
        if (activeFile) {
            const fileContent = activeFile.content
                ? `${activeFile.content}\n`
                : ""
            const content = `${fileContent}${output.replace(/```[\w]*\n?/g, "").trim()}`
            updateFileContent(activeFile.id, content)
            // Update the content of the active file if it's the same file
            setActiveFile({ ...activeFile, content })
            toast.success("Code pasted successfully")
            // Emit the FILE_UPDATED event to the server
            socket.emit(SocketEvent.FILE_UPDATED, {
                fileId: activeFile.id,
                newContent: content,
            })
        }
    }

    const handleReplaceClick = () => {
        if (activeFile) {
            setIsReplaceConfirmOpen(true)
        }
    }

    const confirmReplace = () => {
        if (activeFile) {
            const content = output.replace(/```[\w]*\n?/g, "").trim()
            updateFileContent(activeFile.id, content)
            // Update the content of the active file if it's the same file
            setActiveFile({ ...activeFile, content })
            toast.success("Code replaced successfully")
            // Emit the FILE_UPDATED event to the server
            socket.emit(SocketEvent.FILE_UPDATED, {
                fileId: activeFile.id,
                newContent: content,
            })
        }
        setIsReplaceConfirmOpen(false)
    }

    return (
        <div
            className="flex max-h-full min-h-[400px] w-full flex-col gap-2 p-4"
            style={{ height: viewHeight }}
        >
            <h1 className="view-title">Copilot</h1>
            <textarea
                className="min-h-[120px] w-full rounded-md border-none bg-darkHover p-2 text-white outline-none"
                placeholder="What code do you want to generate?"
                onChange={(e) => setInput(e.target.value)}
            />
            <button
                className="mt-1 flex w-full justify-center rounded-md bg-primary p-2 font-bold text-black outline-none disabled:cursor-not-allowed disabled:opacity-50"
                onClick={generateCode}
                disabled={isRunning}
            >
                {isRunning ? "Generating..." : "Generate Code"}
            </button>
            {output && (
                <div className="flex justify-end gap-4 pt-2">
                    <button title="Copy Output" onClick={copyOutput}>
                        <LuCopy
                            size={18}
                            className="cursor-pointer text-white"
                        />
                    </button>
                    <button
                        title="Replace code in file"
                        onClick={handleReplaceClick}
                    >
                        <LuRepeat
                            size={18}
                            className="cursor-pointer text-white"
                        />
                    </button>
                    <button
                        title="Paste code in file"
                        onClick={pasteCodeInFile}
                    >
                        <LuClipboardPaste
                            size={18}
                            className="cursor-pointer text-white"
                        />
                    </button>
                </div>
            )}
            <div className="h-full rounded-lg w-full overflow-y-auto p-0">
                <ReactMarkdown
                    components={{
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        code({ inline, className, children, ...props }: any) {
                            const match = /language-(\w+)/.exec(className || "")
                            const language = match ? match[1] : "javascript" // Default to JS

                            return !inline ? (
                                <SyntaxHighlighter
                                    style={dracula}
                                    language={language}
                                    PreTag="pre"
                                    className="!m-0 !h-full !rounded-lg !bg-gray-900 !p-2"
                                >
                                    {String(children).replace(/\n$/, "")}
                                </SyntaxHighlighter>
                            ) : (
                                <code className={className} {...props}>
                                    {children}
                                </code>
                            )
                        },
                        pre({ children }) {
                            return <pre className="h-full">{children}</pre>
                        },
                    }}
                >
                    {output}
                </ReactMarkdown>
            </div>

            <Modal
                isOpen={isReplaceConfirmOpen}
                title="Replace code in active file?"
                confirmText="Replace"
                onConfirm={confirmReplace}
                onClose={() => setIsReplaceConfirmOpen(false)}
            />
        </div>
    )
}

export default CopilotView
