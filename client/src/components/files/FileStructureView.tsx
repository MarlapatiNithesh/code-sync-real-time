import { useAppContext } from "@/context/AppContext"
import { useFileSystem } from "@/context/FileContext"
import { useViews } from "@/context/ViewContext"
import { useContextMenu } from "@/hooks/useContextMenu"
import useWindowDimensions from "@/hooks/useWindowDimensions"
import { ACTIVITY_STATE } from "@/types/app"
import { FileSystemItem, Id } from "@/types/file"
import { sortFileSystemItem } from "@/utils/file"
import { getIconClassName } from "@/utils/getIconClassName"
import { Icon } from "@iconify/react"
import cn from "classnames"
import { MouseEvent, useEffect, useRef, useState } from "react"
import { AiOutlineFolder, AiOutlineFolderOpen } from "react-icons/ai"
import { MdDelete } from "react-icons/md"
import { PiPencilSimpleFill } from "react-icons/pi"
import {
    RiFileAddLine,
    RiFolderAddLine,
    RiFolderUploadLine,
} from "react-icons/ri"
import RenameView from "./RenameView"
import useResponsive from "@/hooks/useResponsive"
import { Modal } from "../common/Modal"

function FileStructureView() {
    const { fileStructure, createFile, createDirectory, collapseDirectories } =
        useFileSystem()
    const explorerRef = useRef<HTMLDivElement | null>(null)
    const [selectedDirId, setSelectedDirId] = useState<Id | null>(null)
    const { minHeightReached } = useResponsive()
    const [isCreateFileOpen, setIsCreateFileOpen] = useState(false)
    const [isCreateDirOpen, setIsCreateDirOpen] = useState(false)

    useEffect(() => {
        const handleGlobalShortcuts = (e: KeyboardEvent) => {
            if (
                document.activeElement?.tagName === "INPUT" ||
                document.activeElement?.tagName === "TEXTAREA" ||
                document.activeElement?.hasAttribute("contenteditable")
            ) {
                return
            }

            if (e.altKey && e.key.toLowerCase() === "n") {
                e.preventDefault()
                if (e.shiftKey) {
                    setIsCreateDirOpen(true)
                } else {
                    setIsCreateFileOpen(true)
                }
            }
        }
        window.addEventListener("keydown", handleGlobalShortcuts)
        return () => window.removeEventListener("keydown", handleGlobalShortcuts)
    }, [])

    const handleCreateFile = (fileName?: string) => {
        if (fileName && fileName.trim().length > 0) {
            const parentDirId: Id = selectedDirId || fileStructure.id
            createFile(parentDirId, fileName.trim())
        }
        setIsCreateFileOpen(false)
    }

    const handleCreateDirectory = (dirName?: string) => {
        if (dirName && dirName.trim().length > 0) {
            const parentDirId: Id = selectedDirId || fileStructure.id
            createDirectory(parentDirId, dirName.trim())
        }
        setIsCreateDirOpen(false)
    }

    const sortedFileStructure = sortFileSystemItem(fileStructure)

    return (
        <div className="flex flex-grow flex-col">
            <div className="view-title flex justify-between">
                <h2>Files</h2>
                <div className="flex gap-2">
                    <button
                        className="rounded-md px-1 hover:bg-darkHover"
                        onClick={() => setIsCreateFileOpen(true)}
                        title="Create File"
                    >
                        <RiFileAddLine size={20} />
                    </button>
                    <button
                        className="rounded-md px-1 hover:bg-darkHover"
                        onClick={() => setIsCreateDirOpen(true)}
                        title="Create Directory"
                    >
                        <RiFolderAddLine size={20} />
                    </button>
                    <button
                        className="rounded-md px-1 hover:bg-darkHover"
                        onClick={collapseDirectories}
                        title="Collapse All Directories"
                    >
                        <RiFolderUploadLine size={20} />
                    </button>
                </div>
            </div>
            <div
                className={cn(
                    "min-h-[200px] flex-grow overflow-auto pr-2 sm:min-h-0",
                    {
                        "h-[calc(80vh-170px)]": !minHeightReached,
                        "h-[85vh]": minHeightReached,
                    },
                )}
                ref={explorerRef}
                onClick={(e) => {
                    if (e.target === explorerRef.current) {
                        setSelectedDirId(fileStructure.id)
                    }
                }}
            >
                {sortedFileStructure.children &&
                    sortedFileStructure.children.map((item) => (
                        <Directory
                            key={item.id}
                            item={item}
                            selectedDirId={selectedDirId}
                            setSelectedDirId={setSelectedDirId}
                        />
                    ))}
            </div>

            <Modal
                isOpen={isCreateFileOpen}
                title="Create New File"
                placeholder="index.js"
                showInput={true}
                onConfirm={handleCreateFile}
                onClose={() => setIsCreateFileOpen(false)}
            />
            <Modal
                isOpen={isCreateDirOpen}
                title="Create New Directory"
                placeholder="src"
                showInput={true}
                onConfirm={handleCreateDirectory}
                onClose={() => setIsCreateDirOpen(false)}
            />
        </div>
    )
}

function Directory({
    item,
    selectedDirId,
    setSelectedDirId,
}: {
    item: FileSystemItem
    selectedDirId: Id | null
    setSelectedDirId: (id: Id) => void
}) {
    const [isEditing, setEditing] = useState<boolean>(false)
    const [isDeleteOpen, setIsDeleteOpen] = useState<boolean>(false)
    const dirRef = useRef<HTMLDivElement | null>(null)
    const { coords, menuOpen, setMenuOpen } = useContextMenu({
        ref: dirRef,
    })
    const { deleteDirectory, toggleDirectory } = useFileSystem()

    const handleDirClick = (dirId: string) => {
        setSelectedDirId(dirId)
        toggleDirectory(dirId)
    }

    const handleRenameDirectory = (e: MouseEvent) => {
        e.stopPropagation()
        setMenuOpen(false)
        setEditing(true)
    }

    const handleDeleteDirectory = (e: MouseEvent) => {
        e.stopPropagation()
        setMenuOpen(false)
        setIsDeleteOpen(true)
    }

    const confirmDeleteDir = () => {
        deleteDirectory(item.id)
        setIsDeleteOpen(false)
    }

    useEffect(() => {
        const dirNode = dirRef.current

        if (!dirNode) return

        dirNode.tabIndex = 0

        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === "F2") {
                e.stopPropagation()
                setEditing(true)
            } else if (e.key === "Delete") {
                e.stopPropagation()
                setIsDeleteOpen(true)
            }
        }

        dirNode.addEventListener("keydown", handleKeyDown)

        return () => {
            dirNode.removeEventListener("keydown", handleKeyDown)
        }
    }, [])

    if (item.type === "file") {
        return (
            <File
                item={item}
                selectedDirId={selectedDirId}
                setSelectedDirId={setSelectedDirId}
            />
        )
    }

    const isSelected = selectedDirId === item.id

    return (
        <div className="overflow-x-auto">
            <div
                className={cn(
                    "flex w-full items-center rounded-md px-2 py-1 hover:bg-darkHover",
                    { "bg-darkHover": isSelected }
                )}
                onClick={() => handleDirClick(item.id)}
                ref={dirRef}
            >
                {item.isOpen ? (
                    <AiOutlineFolderOpen size={24} className="mr-2 min-w-fit" />
                ) : (
                    <AiOutlineFolder size={24} className="mr-2 min-w-fit" />
                )}
                {isEditing ? (
                    <RenameView
                        id={item.id}
                        preName={item.name}
                        type="directory"
                        setEditing={setEditing}
                    />
                ) : (
                    <p
                        className="flex-grow cursor-pointer overflow-hidden truncate"
                        title={item.name}
                    >
                        {item.name}
                    </p>
                )}
            </div>
            <div
                className={cn(
                    { hidden: !item.isOpen },
                    { block: item.isOpen },
                    { "pl-4": item.name !== "root" },
                )}
            >
                {item.children &&
                    item.children.map((item) => (
                        <Directory
                            key={item.id}
                            item={item}
                            selectedDirId={selectedDirId}
                            setSelectedDirId={setSelectedDirId}
                        />
                    ))}
            </div>

            {menuOpen && (
                <DirectoryMenu
                    handleDeleteDirectory={handleDeleteDirectory}
                    handleRenameDirectory={handleRenameDirectory}
                    left={coords.x}
                    top={coords.y}
                />
            )}

            <Modal
                isOpen={isDeleteOpen}
                title={`Delete directory "${item.name}"?`}
                confirmText="Delete"
                onConfirm={confirmDeleteDir}
                onClose={() => setIsDeleteOpen(false)}
            />
        </div>
    )
}

const File = ({
    item,
    selectedDirId,
    setSelectedDirId,
}: {
    item: FileSystemItem
    selectedDirId: Id | null
    setSelectedDirId: (id: Id) => void
}) => {
    const { deleteFile, openFile } = useFileSystem()
    const [isEditing, setEditing] = useState<boolean>(false)
    const [isDeleteOpen, setIsDeleteOpen] = useState<boolean>(false)
    const { setIsSidebarOpen } = useViews()
    const { isMobile } = useWindowDimensions()
    const { activityState, setActivityState } = useAppContext()
    const fileRef = useRef<HTMLDivElement | null>(null)
    const { menuOpen, coords, setMenuOpen } = useContextMenu({
        ref: fileRef,
    })

    const handleFileClick = (fileId: string) => {
        if (isEditing) return
        setSelectedDirId(fileId)

        openFile(fileId)
        if (isMobile) {
            setIsSidebarOpen(false)
        }
        if (activityState === ACTIVITY_STATE.DRAWING) {
            setActivityState(ACTIVITY_STATE.CODING)
        }
    }

    const handleRenameFile = (e: MouseEvent) => {
        e.stopPropagation()
        setEditing(true)
        setMenuOpen(false)
    }

    const handleDeleteFile = (e: MouseEvent) => {
        e.stopPropagation()
        setMenuOpen(false)
        setIsDeleteOpen(true)
    }

    const confirmDeleteFile = () => {
        deleteFile(item.id)
        setIsDeleteOpen(false)
    }

    useEffect(() => {
        const fileNode = fileRef.current

        if (!fileNode) return

        fileNode.tabIndex = 0

        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === "F2") {
                e.stopPropagation()
                setEditing(true)
            } else if (e.key === "Delete") {
                e.stopPropagation()
                setIsDeleteOpen(true)
            }
        }

        fileNode.addEventListener("keydown", handleKeyDown)

        return () => {
            fileNode.removeEventListener("keydown", handleKeyDown)
        }
    }, [])

    const isSelected = selectedDirId === item.id

    return (
        <div
            className={cn(
                "flex w-full items-center rounded-md px-2 py-1 hover:bg-darkHover",
                { "bg-darkHover": isSelected }
            )}
            onClick={() => handleFileClick(item.id)}
            ref={fileRef}
        >
            <Icon
                icon={getIconClassName(item.name)}
                fontSize={22}
                className="mr-2 min-w-fit"
            />
            {isEditing ? (
                <RenameView
                    id={item.id}
                    preName={item.name}
                    type="file"
                    setEditing={setEditing}
                />
            ) : (
                <p
                    className="flex-grow cursor-pointer overflow-hidden truncate"
                    title={item.name}
                >
                    {item.name}
                </p>
            )}

            {menuOpen && (
                <FileMenu
                    top={coords.y}
                    left={coords.x}
                    handleRenameFile={handleRenameFile}
                    handleDeleteFile={handleDeleteFile}
                />
            )}

            <Modal
                isOpen={isDeleteOpen}
                title={`Delete file "${item.name}"?`}
                confirmText="Delete"
                onConfirm={confirmDeleteFile}
                onClose={() => setIsDeleteOpen(false)}
            />
        </div>
    )
}

const FileMenu = ({
    top,
    left,
    handleRenameFile,
    handleDeleteFile,
}: {
    top: number
    left: number
    handleRenameFile: (e: MouseEvent) => void
    handleDeleteFile: (e: MouseEvent) => void
}) => {
    return (
        <div
            className="absolute z-10 w-[150px] rounded-md border border-darkHover bg-dark p-1"
            style={{
                top,
                left,
                pointerEvents: "auto",
            }}
        >
            <button
                onClick={handleRenameFile}
                className="flex w-full items-center gap-2 rounded-md px-2 py-1 hover:bg-darkHover"
            >
                <PiPencilSimpleFill size={18} />
                Rename
            </button>
            <button
                onClick={handleDeleteFile}
                className="flex w-full items-center gap-2 rounded-md px-2 py-1 text-danger hover:bg-darkHover"
            >
                <MdDelete size={20} />
                Delete
            </button>
        </div>
    )
}

const DirectoryMenu = ({
    top,
    left,
    handleRenameDirectory,
    handleDeleteDirectory,
}: {
    top: number
    left: number
    handleRenameDirectory: (e: MouseEvent) => void
    handleDeleteDirectory: (e: MouseEvent) => void
}) => {
    return (
        <div
            className="absolute z-10 w-[150px] rounded-md border border-darkHover bg-dark p-1"
            style={{
                top,
                left,
                pointerEvents: "auto",
            }}
        >
            <button
                onClick={handleRenameDirectory}
                className="flex w-full items-center gap-2 rounded-md px-2 py-1 hover:bg-darkHover"
            >
                <PiPencilSimpleFill size={18} />
                Rename
            </button>
            <button
                onClick={handleDeleteDirectory}
                className="flex w-full items-center gap-2 rounded-md px-2 py-1 text-danger hover:bg-darkHover"
            >
                <MdDelete size={20} />
                Delete
            </button>
        </div>
    )
}

export default FileStructureView
