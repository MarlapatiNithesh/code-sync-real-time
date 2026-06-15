import React, { useEffect, useRef } from "react"
import { createPortal } from "react-dom"

interface ModalProps {
    isOpen: boolean
    title: string
    defaultValue?: string
    placeholder?: string
    confirmText?: string
    cancelText?: string
    showInput?: boolean
    onConfirm: (inputValue?: string) => void
    onClose: () => void
}

export const Modal: React.FC<ModalProps> = ({
    isOpen,
    title,
    defaultValue = "",
    placeholder = "",
    confirmText = "OK",
    cancelText = "Cancel",
    showInput = false,
    onConfirm,
    onClose,
}) => {
    const inputRef = useRef<HTMLInputElement | null>(null)

    useEffect(() => {
        if (isOpen && showInput && inputRef.current) {
            setTimeout(() => {
                inputRef.current?.focus()
                inputRef.current?.select()
            }, 50)
        }
    }, [isOpen, showInput])

    if (!isOpen) return null

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        onConfirm(inputRef.current?.value || "")
    }

    return createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 transition-all duration-300">
            <div className="w-full max-w-[450px] transform rounded-lg border border-darkHover bg-[#1E2024] p-6 shadow-2xl transition-all">
                <h3 className="text-xl font-semibold text-light mb-4">{title}</h3>
                <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                    {showInput && (
                        <input
                            ref={inputRef}
                            type="text"
                            defaultValue={defaultValue}
                            placeholder={placeholder}
                            className="w-full rounded-md border border-gray-600 bg-darkHover px-4 py-3 text-light placeholder-gray-400 focus:border-primary focus:outline-none"
                            required
                        />
                    )}
                    <div className="flex justify-end gap-3 mt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-md border border-gray-500 px-4 py-2 text-sm font-medium text-light hover:bg-darkHover transition"
                        >
                            {cancelText}
                        </button>
                        <button
                            type="submit"
                            className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-black hover:opacity-90 transition"
                        >
                            {confirmText}
                        </button>
                    </div>
                </form>
            </div>
        </div>,
        document.body
    )
}
