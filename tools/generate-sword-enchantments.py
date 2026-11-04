"""Vanilla1.21 enchantment definitions and a narrow optional Simply Swords item bridge."""
from pathlib import Path
from zipfile import ZipFile
import json
root=Path(__file__).resolve().parents[1];data=root/'src/main/resources/data';assets=root/'src/main/resources/assets/witchersbestiary'
def write(path,value):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
ids=['monsters_bane','frost_blade','blood_harvest','runic_resonance']
for id,weight,base,step,anvil in [('monsters_bane',5,5,9,2),('frost_blade',3,10,8,3),('blood_harvest',1,18,7,4),('runic_resonance',1,20,8,4)]:
 d={'description':{'translate':'enchantment.witchersbestiary.'+id},'supported_items':'#witchersbestiary:enchantable/hunting_swords','primary_items':'#witchersbestiary:enchantable/hunting_swords','weight':weight,'max_level':3,'min_cost':{'base':base,'per_level_above_first':step},'max_cost':{'base':base+20,'per_level_above_first':step},'anvil_cost':anvil,'slots':['mainhand'],'effects':{}}
 if id=='monsters_bane':d['exclusive_set']='#minecraft:exclusive_set/damage'
 if id=='frost_blade':d['exclusive_set']='#witchersbestiary:exclusive_set/thermal'
 write(data/f'witchersbestiary/enchantment/{id}.json',d)
write(data/'minecraft/tags/enchantment/non_treasure.json',{'replace':False,'values':['witchersbestiary:'+id for id in ids]})
write(data/'minecraft/tags/enchantment/exclusive_set/damage.json',{'replace':False,'values':['witchersbestiary:monsters_bane']})
write(data/'witchersbestiary/tags/enchantment/exclusive_set/thermal.json',{'replace':False,'values':['witchersbestiary:frost_blade','minecraft:fire_aspect']})
write(data/'witchersbestiary/tags/item/enchantable/hunting_swords.json',{'replace':False,'values':['#minecraft:swords','#witchersbestiary:enchantable/simply_swords']})
selected=set()
with ZipFile(root/'build/stage20e-compat/mods/simplyswords-fabric-1.63.0-1.21.1.jar') as z:
 for n in z.namelist():
  if '/weapon_attributes/' not in n or not n.endswith('.json'):continue
  short=n.split('/weapon_attributes/')[1][:-5]
  if '/' in short:continue
  d=json.loads(z.read(n));cat=d.get('attributes',{}).get('category')
  if cat in ['sword','claymore','rapier','katana'] or any(short==material+'_'+family for material in ['iron','gold','diamond','netherite','runic'] for family in ['longsword','claymore','rapier','katana','cutlass','twinblade']):selected.add('simplyswords:'+short)
write(data/'witchersbestiary/tags/item/enchantable/simply_swords.json',{'replace':False,'values':[{'id':id,'required':False} for id in sorted(selected)]})
write(data/'witchersbestiary/damage_type/runic_pulse.json',{'message_id':'runic_pulse','scaling':'never','exhaustion':0.0})
write(data/'minecraft/tags/damage_type/bypasses_armor.json',{'replace':False,'values':['witchersbestiary:runic_pulse']})
translations={'en_us':["Monster's Bane",'Frost Blade','Blood Harvest','Runic Resonance'],'ru_ru':['Гроза чудовищ','Морозный клинок','Кровавая жатва','Рунический резонанс']}
for lang,names in translations.items():
 p=assets/f'lang/{lang}.json';d=json.loads(p.read_text(encoding='utf-8'));d.update({'enchantment.witchersbestiary.'+id:name for id,name in zip(ids,names)});d['tooltip.witchersbestiary.runic_charge']='Runic charge: %s' if lang=='en_us' else 'Рунический заряд: %s';d['death.attack.runic_pulse']='%1$s was struck by a runic pulse' if lang=='en_us' else '%1$s поражён руническим импульсом';d['death.attack.runic_pulse.player']='%1$s was struck by %2$s\'s runic pulse' if lang=='en_us' else '%1$s поражён руническим импульсом игрока %2$s';write(p,d)
print('Four vanilla enchantment definitions; optional eligible Simply Swords:',len(selected))
