import xml.etree.ElementTree as et
result = ''

filename = 'vbatch_tables.architect'
root = et.parse('vbatch_tables.architect')

# How to make decisions based on attributes even in 2.6
for e in root.findall('//table'):
    lc = e.attrib.get('name').lower()
    print e.attrib.get('name')
    e.set('name',lc)
    p_lc = e.attrib.get('physicalName').lower()
    e.set('physicalName',p_lc)
    if e.attrib.get('name') == 'foo':
        result = e.text
        break
   
root.write(filename)
