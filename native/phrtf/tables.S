    .section .rodata
    .global defaultHannWin
    .hidden defaultHannWin
    .type defaultHannWin, @object
    .align 4
defaultHannWin:
    .incbin "hann_win.bin"
    .size defaultHannWin, . - defaultHannWin

    .section .data
    .global sos44
    .type sos44, @object
    .align 4
sos44:
    .incbin "sos44.bin"
    .size sos44, . - sos44

    .global sos48
    .type sos48, @object
    .align 4
sos48:
    .incbin "sos48.bin"
    .size sos48, . - sos48
