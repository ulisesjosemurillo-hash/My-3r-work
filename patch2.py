import sys

with open(sys.argv[1], "r") as f:
    text = f.read()

target = """        Text(
            text = "JurisTech Reader",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = JtPrimaryText,
            textAlign = TextAlign.Center
        )"""

replacement = """        Text(
            text = "JurisTech",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = JtPrimaryText,
            textAlign = TextAlign.Center
        )
        Text(
            text = "R E A D E R",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = JtPrimaryText,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.width(40.dp).height(2.dp).background(JtGold))"""

if target in text:
    text = text.replace(target, replacement)
    with open(sys.argv[1], "w") as f:
        f.write(text)
    print("Patched EmptyLibraryView")
else:
    print("Target 1 not found")
