         stime[RECEPTION] = sgen.nextDouble()*STIMEMULT[RECEPTION];
         acwTime[RECEPTION] = acwgen.nextDouble();
         if (uConfAccounting < PROBCONFACCOUNTING)
            stime[ACCOUNTING] = confGen.nextDouble()*STIMEMULT[ACCOUNTING];
         //else
         //   stime[ACCOUNTING] = 0;
         //acwTime[ACCOUNTING] = 0;
         //acwTime[DEV] = 0;
         if (uTransManager < PROBTRANSMANAGER) {
            pManager = pgen.nextDouble();
            stime[MANAGER] = sgen.nextDouble()*STIMEMULT[MANAGER];
            acwTime[MANAGER] = acwgen.nextDouble();
            //stime[TECHSUP] = 0;
            //stime[DEV] = 0;
         }
         else {
            //pManager = 0;
            //stime[MANAGER] = 0;
            //acwTime[MANAGER] = 0;
            stime[TECHSUP] = sgen.nextDouble()*STIMEMULT[TECHSUP];
            acwTime[TECHSUP] = acwgen.nextDouble();
            uConfDev = probConfDevStream.nextDouble();
            if (uConfDev < PROBCONFDEV)
               stime[DEV] = confGen.nextDouble()*STIMEMULT[DEV];
            //else
            //   stime[DEV] = 0;
         }
