
import sys

def check_braces(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    stack = []
    for i, line in enumerate(lines):
        for char_index, char in enumerate(line):
            if char == '{':
                stack.append((i + 1, char_index + 1))
            elif char == '}':
                if not stack:
                    print(f"Error: Unexpected closing brace at Line {i + 1}, Col {char_index + 1}")
                    return
                stack.pop()
    
    if stack:
        print(f"Error: Unclosed braces remaining at EOF. Count: {len(stack)}")
        for item in stack:
             print(f"  - Opened at Line {item[0]}")
    else:
        print("Success: Braces are balanced.")

if __name__ == "__main__":
    check_braces(sys.argv[1])
