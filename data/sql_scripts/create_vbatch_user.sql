CREATE TABLESPACE vbatch_dev
  DATAFILE 'vbatch_dev.dat' 
    SIZE 10M
    REUSE
    AUTOEXTEND ON NEXT 10M MAXSIZE 2000M;
    
CREATE USER vbatch
  IDENTIFIED BY vbatch
  DEFAULT TABLESPACE vbatch_dev
  QUOTA 2000M on vbatch_dev;
  
  GRANT create session TO vbatch;
  GRANT create table TO vbatch;
GRANT create view TO vbatch;
GRANT create any trigger TO vbatch;
GRANT create any procedure TO vbatch;
GRANT create sequence TO vbatch;
GRANT create synonym TO vbatch;