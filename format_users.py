import json

try:
    with open('/home/grym/.gemini/antigravity-cli/brain/78effef3-4db9-46b0-8e8c-1769bf233a14/UserData.md', 'w') as out:
        out.write("# User Data\n\n")
        
        with open('/tmp/users.jsonl', 'r') as f:
            lines = f.readlines()
            
            if not lines:
                out.write("No users found.\n")
            else:
                for line in lines:
                    user = json.loads(line.strip())
                    out.write(f"## User ID: {user.get('id')}\n")
                    out.write(f"- **Phone Number:** {user.get('phone_number')}\n")
                    out.write(f"- **Role:** {user.get('role')}\n")
                    out.write(f"- **Created At:** {user.get('created_at')}\n")
                    out.write(f"- **Blocked:** {user.get('blocked')}\n")
                    out.write(f"- **Store ID:** {user.get('store_id')}\n")
                    out.write(f"- **Total Booked Slots:** {user.get('total_booked_slots')}\n")
                    out.write(f"- **API Key:** {user.get('api_key')}\n\n")
                    
                    out.write("### User Headers\n")
                    headers = user.get('user_headers')
                    if headers:
                        out.write("```json\n")
                        out.write(json.dumps(headers, indent=2))
                        out.write("\n```\n")
                    else:
                        out.write("No headers found.\n")
                    
                    out.write("\n---\n")
except Exception as e:
    print(f"Error: {e}")
