BEGIN
     --Bye triggers
  FOR i in (select trigger_name,table_owner 
              from user_triggers 
             where  table_owner = 'VBATCH' ) LOOP  
    EXECUTE IMMEDIATE 'DROP TRIGGER '||i.table_owner||'.'||i.trigger_name;  
  END LOOP; 
END;
/