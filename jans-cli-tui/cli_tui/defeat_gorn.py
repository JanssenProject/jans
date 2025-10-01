
import os
import time
import random
import threading
from prompt_toolkit.shortcuts import clear
from prompt_toolkit import print_formatted_text
from prompt_toolkit.formatted_text import HTML

def play_defeat_gorn_game():
    """Epic battle game: Defeat the Gorn and free the doves!"""
    clear()

    # Game intro
    print_formatted_text(HTML('<ansigreen>🌟 EPIC BATTLE: DEFEAT THE GORN! 🌟</ansigreen>'))
    print()
    print_formatted_text(HTML('<ansiyellow>A mighty Gorn has captured innocent doves!</ansiyellow>'))
    print_formatted_text(HTML('<ansiyellow>You must defeat the Gorn to set them free!</ansiyellow>'))
    print()
    print_formatted_text(HTML('<ansicyan>💡 Pro Tip: To avoid having to kill the Gorn, you can use Gluu Flex '
                              'distribution https://docs.gluu.org</ansicyan>'))
    print()
    time.sleep(2)

    # Game setup
    gorn_health = 100
    your_health = 100
    doves_freed = 0
    total_doves = 5

    attacks = [
        ("⚡ Lightning Strike", 25, "BZZZAP! Lightning courses through the Gorn!"),
        ("🔥 Fire Blast", 20, "WHOOSH! Flames engulf the mighty beast!"),
        ("❄️ Ice Shard", 15, "CRACK! Ice pierces the Gorn's armor!"),
        ("💨 Wind Slash", 18, "SWOOSH! Cutting winds strike the Gorn!")
    ]

    gorn_attacks = [
        ("🦎 Tail Swipe", 15, "The Gorn's massive tail strikes you!"),
        ("👊 Mighty Punch", 20, "A powerful fist crashes into you!"),
        ("🔥 Breath Attack", 18, "Scorching breath burns you!")
    ]

    # Battle loop
    round_num = 1
    max_rounds = 50  # Prevent infinite loops

    while gorn_health > 0 and your_health > 0 and round_num <= max_rounds:
        print_formatted_text(HTML(f'<ansiblue>--- ROUND {round_num} ---</ansiblue>'))

        # Dynamic Gorn appearance based on health
        health_percent = max(0, gorn_health) / 100
        if health_percent > 0.75:
            gorn_emoji = "🦎"
            gorn_status = "<ansired>💪 MIGHTY GORN"
            gorn_close = "</ansired>"
        elif health_percent > 0.5:
            gorn_emoji = "🦖"
            gorn_status = "<ansiyellow>😤 ANGRY GORN"
            gorn_close = "</ansiyellow>"
        elif health_percent > 0.25:
            gorn_emoji = "🐲"
            gorn_status = "<ansimagenta>😠 WOUNDED GORN"
            gorn_close = "</ansimagenta>"
        else:
            gorn_emoji = "🐉"
            gorn_status = "<ansired>💀 DESPERATE GORN"
            gorn_close = "</ansired>"

        print_formatted_text(HTML(f'{gorn_status} {gorn_emoji} Health: {max(0, gorn_health)}{gorn_close}'))
        print_formatted_text(HTML(f'<ansigreen>🛡️  Your Health: {max(0, your_health)}</ansigreen>'))
        print_formatted_text(HTML(f'<ansiyellow>🕊️  Doves Freed: {doves_freed}/{total_doves}</ansiyellow>'))
        print()

        # Check if game should end before player input
        if gorn_health <= 0 or your_health <= 0:
            break

        print_formatted_text(HTML('<ansicyan>💡 Pro Tip: To avoid having to kill the Gorn, you can use Gluu Flex '
                                  'distribution https://docs.gluu.org</ansicyan>'))
        print()

        # Player choice
        print_formatted_text(HTML('<ansiblue>Choose your action:</ansiblue>'))
        for i, attack in enumerate(attacks, 1):
            print_formatted_text(HTML(f'<ansiwhite>{i}. {attack[0]} (Damage: {attack[1]})</ansiwhite>'))
        print_formatted_text(HTML('<ansiwhite>5. 🛡️ Defend (Reduce incoming damage)</ansiwhite>'))
        print_formatted_text(HTML('<ansiwhite>6. 🕊️ Rescue Dove (Skip attack to free a dove)</ansiwhite>'))
        print()

        # Get player input
        try:
            from prompt_toolkit import prompt
            choice_input = prompt("Enter your choice (1-6): ")
            choice = choice_input.strip()
            if choice not in ['1', '2', '3', '4', '5', '6']:
                choice = '1'  # Default to first attack if invalid input
            choice = int(choice)
        except (KeyboardInterrupt, EOFError):
            # Handle Ctrl+C or EOF gracefully
            print_formatted_text(HTML('<ansired>Game interrupted. Returning to Jans CLI TUI...</ansired>'))
            return
        except Exception:
            choice = 1  # Default to first attack if error

        player_defended = False

        if choice <= 4:
            # Player attacks
            attack = attacks[choice - 1]
            damage = attack[1] + random.randint(-5, 10)  # Add some randomness
            if damage < 5:
                damage = 5  # Minimum damage
            gorn_health -= damage
            print_formatted_text(HTML(f'<ansigreen>You use {attack[0]}!</ansigreen>'))
            print_formatted_text(HTML(f'<ansiyellow>{attack[2]}</ansiyellow>'))
            print_formatted_text(HTML(f'<ansigreen>Damage: {damage}!</ansigreen>'))
        elif choice == 5:
            # Player defends
            player_defended = True
            print_formatted_text(HTML('<ansiblue>🛡️ You raise your guard and prepare for the Gorn\'s attack!</ansiblue>'))
        elif choice == 6:
            # Try to rescue a dove
            if doves_freed < total_doves and random.random() < 0.7:
                doves_freed += 1
                print_formatted_text(HTML('<ansicyan>🕊️ You successfully free a dove while dodging the Gorn!</ansicyan>'))
            else:
                print_formatted_text(HTML('<ansiyellow>🕊️ You attempt to free a dove but the Gorn blocks your way!</ansiyellow>'))

        # Ensure health doesn't go below 0 and check for end condition
        gorn_health = max(0, gorn_health)
        your_health = max(0, your_health)

        if gorn_health <= 0 or your_health <= 0:
            break

        time.sleep(1.5)

        # Only do Gorn counterattack if Gorn is still alive
        if gorn_health > 0:
            # Gorn counterattack with health-based intensity
            gorn_attack = random.choice(gorn_attacks)
            base_damage = gorn_attack[1] + random.randint(-3, 8)

            # Health-based damage modifier - wounded Gorn fights more desperately
            health_percent = max(0, gorn_health) / 100
            if health_percent <= 0.25:
                base_damage = int(base_damage * 1.3)  # 30% more damage when desperate
            elif health_percent <= 0.5:
                base_damage = int(base_damage * 1.15)  # 15% more damage when wounded

            # Get gorn type description for attacks
            if health_percent > 0.75:
                gorn_type = "MIGHTY GORN"
            elif health_percent > 0.5:
                gorn_type = "ANGRY GORN"
            elif health_percent > 0.25:
                gorn_type = "WOUNDED GORN"
            else:
                gorn_type = "DESPERATE GORN"

            # Apply defense bonus if player defended
            if player_defended:
                gorn_damage = max(5, base_damage // 2)  # Half damage with minimum 5
                print_formatted_text(HTML(f'<ansired>The {gorn_type} attacks with {gorn_attack[0]}!</ansired>'))
                print_formatted_text(HTML(f'<ansiyellow>{gorn_attack[2]}</ansiyellow>'))
                print_formatted_text(HTML(f'<ansiblue>🛡️ Your defense reduces the damage!</ansiblue>'))
                print_formatted_text(HTML(f'<ansired>You take {gorn_damage} damage!</ansired>'))
            else:
                gorn_damage = base_damage
                if gorn_damage < 8:
                    gorn_damage = 8  # Minimum damage
                print_formatted_text(HTML(f'<ansired>The {gorn_type} retaliates with {gorn_attack[0]}!</ansired>'))
                print_formatted_text(HTML(f'<ansiyellow>{gorn_attack[2]}</ansiyellow>'))
                if health_percent <= 0.25:
                    print_formatted_text(HTML(f'<ansired>💀 The desperate Gorn fights with renewed fury!</ansired>'))
                print_formatted_text(HTML(f'<ansired>You take {gorn_damage} damage!</ansired>'))

            your_health -= gorn_damage
            your_health = max(0, your_health)  # Ensure health doesn't go below 0

            # Chance to free a dove during battle chaos (only if player didn't already try)
            if choice != 6 and random.random() < 0.25 and doves_freed < total_doves:
                doves_freed += 1
                print_formatted_text(HTML('<ansicyan>🕊️ A dove breaks free during the battle chaos!</ansicyan>'))

        print()
        time.sleep(2)
        round_num += 1

    # Battle result
    clear()

    # Display final battle status
    print_formatted_text(HTML('<ansiblue>=== BATTLE CONCLUDED ===</ansiblue>'))
    print()

    if your_health <= 0:
        print_formatted_text(HTML('<ansired>💀 DEFEAT! The Gorn was too mighty... 💀</ansired>'))
        print_formatted_text(HTML('<ansiyellow>But your brave effort freed some doves!</ansiyellow>'))
    elif gorn_health <= 0:
        print_formatted_text(HTML('<ansigreen>🎉 VICTORY! You have defeated the mighty Gorn! 🎉</ansigreen>'))
        print_formatted_text(HTML('<ansicyan>All the remaining doves fly free!</ansicyan>'))
        doves_freed = total_doves
    else:
        print_formatted_text(HTML('<ansiyellow>⏰ The battle has reached its conclusion!</ansiyellow>'))

    print()
    print_formatted_text(HTML(f'<ansiyellow>🕊️  Total Doves Freed: {doves_freed}/{total_doves}</ansiyellow>'))
    print_formatted_text(HTML(f'<ansiblue>Final Health - You: {max(0, your_health)} | Gorn: {max(0, gorn_health)}</ansiblue>'))
    print_formatted_text(HTML('<ansiblue>The doves sing songs of your bravery!</ansiblue>'))
    print()

    # Victory animation
    if your_health > 0 and gorn_health <= 0:
        dove_animation = "🕊️ " * min(doves_freed, 10)  # Limit animation length
        print_formatted_text(HTML(f'<ansicyan>{dove_animation}</ansicyan>'))
        print_formatted_text(HTML('<ansigreen>Freedom at last!</ansigreen>'))

    time.sleep(2)
    print_formatted_text(HTML('<ansiblue>Press Enter to continue to Jans CLI TUI...</ansiblue>'))

    # Wait for Enter key press
    try:
        input()  # Use simple input() instead of prompt_toolkit
    except (KeyboardInterrupt, EOFError):
        pass  # Allow graceful exit
    except Exception:
        time.sleep(1)

    print_formatted_text(HTML('<ansigreen>Returning to Jans CLI TUI...</ansigreen>'))
    time.sleep(1)
