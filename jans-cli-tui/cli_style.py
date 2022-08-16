from prompt_toolkit.styles import Style

style = Style.from_dict(
    {
        "window.border": "#888888",
        "shadow": "bg:#222222",
        "menu-bar": "bg:#aaaaaa #888888",
        "menu-bar.selected-item": "bg:#ffffff #000000",
        "menu": "bg:#888888 #ffffff",
        "menu.border": "#aaaaaa",
        "window.border shadow": "#444444",
        "focused  button": "bg:#880000 #ffffff noinherit",
        # Styling for Dialog widgets.
        "button-bar": "bg:#4D4D4D",
        "text-area focused": "bg:#ff0000",
        "status": "reverse",
        "select-box cursor-line": "nounderline bg:ansired fg:ansiwhite",
        "textarea":"nounderline bg:#ffffff fg:#d1c0c0 #ff0000",
        "checkbox":"nounderline bg:#ffffff fg:#d1c0c0 #ff0000",
    }
)