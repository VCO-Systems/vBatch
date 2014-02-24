BEGIN
     --Bye triggers
  FOR i in (select trigger_name,owner 
              from dba_triggers 
             where  owner = 'vbatch' ) LOOP  
    EXECUTE IMMEDIATE 'DROP TRIGGER '||i.owner||'.'||i.trigger_name;  
  END LOOP; 
END;
/