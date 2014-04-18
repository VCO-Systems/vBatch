find output/ -maxdepth 1 -ctime +30 -daystart -exec rm "{}" \;
find logs/ -maxdepth 1 -ctime +30 -daystart -exec rm "{}" \;
