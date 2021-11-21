window.deltachat = {
    getChatName: () => W30.getChatName(),
    getPreferredLocale: () => undefined, // not implemented yet
    isDarkThemePreferred: () => W30.preferDarkMode()
}


document.writeln("hi")
document.writeln("Chat name: " + window.deltachat.getChatName())