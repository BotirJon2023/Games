import { StoryScene } from '../types/game';
import { enemies } from './enemies';

export const storyScenes: StoryScene[] = [
  {
    id: 'intro',
    title: 'The Beginning',
    text: 'You wake up in a dimly lit tavern, your head throbbing. The last thing you remember is accepting a mysterious quest from a hooded stranger. Outside, thunder rumbles ominously. The tavern keeper eyes you warily.',
    choices: [
      { text: 'Ask the tavern keeper about the quest', action: 'tavern_info' },
      { text: 'Head outside into the storm', action: 'forest_entrance' },
      { text: 'Check your belongings', action: 'inventory_check' }
    ]
  },
  {
    id: 'tavern_info',
    title: 'The Tavern Keeper\'s Tale',
    text: '"Aye, that hooded figure," the keeper says, polishing a mug nervously. "Spoke of ancient ruins in the Whispering Woods. Said there\'s treasure beyond imagination, but also... darker things. Many have ventured forth. Few return."',
    choices: [
      { text: 'Ask about the ruins', action: 'ruins_info' },
      { text: 'Leave for the forest immediately', action: 'forest_entrance' },
      { text: 'Buy supplies first', action: 'tavern_shop' }
    ]
  },
  {
    id: 'forest_entrance',
    title: 'The Whispering Woods',
    text: 'The forest looms before you, ancient trees creaking in the wind. Their branches form twisted shapes against the stormy sky. A narrow path winds deeper into the darkness, while strange sounds echo from within.',
    choices: [
      { text: 'Follow the main path cautiously', action: 'forest_path' },
      { text: 'Search for a different route', action: 'forest_alternate' },
      { text: 'Make camp and wait for daylight', action: 'forest_camp' }
    ]
  },
  {
    id: 'forest_path',
    title: 'An Unwelcome Encounter',
    text: 'As you venture deeper into the woods, branches snap behind you. A low growl emanates from the shadows, and glowing eyes peer through the darkness. A goblin warrior emerges, weapon drawn!',
    choices: [
      { text: 'Draw your weapon and fight!', action: 'combat_goblin' },
      { text: 'Try to negotiate', action: 'goblin_talk' },
      { text: 'Attempt to flee', action: 'goblin_flee' }
    ],
    enemy: enemies.find(e => e.id === 'goblin')
  },
  {
    id: 'victory_goblin',
    title: 'Victory!',
    text: 'The goblin falls with a final shriek. You catch your breath and search the area, finding some gold coins scattered about. The path ahead seems clearer now.',
    choices: [
      { text: 'Continue deeper into the forest', action: 'forest_deeper' },
      { text: 'Rest and tend to your wounds', action: 'forest_rest' }
    ],
    autoProgress: true
  },
  {
    id: 'forest_deeper',
    title: 'The Ancient Ruins',
    text: 'After hours of walking, you discover the ruins the stranger mentioned. Crumbling stone pillars stretch toward the sky, covered in glowing runes. At the center stands an ornate door, sealed with magical energy.',
    choices: [
      { text: 'Examine the runes closely', action: 'rune_study' },
      { text: 'Try to force the door open', action: 'door_force' },
      { text: 'Look for another entrance', action: 'ruins_explore' }
    ]
  }
];