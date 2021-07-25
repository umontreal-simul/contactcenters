CPU time: 0:0:1.56
REPORT on Tally stat. collector ==> Number of tried outbound calls
    num. obs.      min          max        average     standard dev.
      1000       52.000     1670.000     1109.040      258.167

Report for Number of arrived contacts
            num obs.     min        max        average     std. dev.   conf. int.
Inbound     1000      425.000    1564.000      848.592     156.201   95.0% (  838.899,  858.285)
Outbound    1000       21.000     475.000      321.896      74.396   95.0% (  317.279,  326.513)
All types   1000      852.000    1586.000     1170.488      94.589   95.0% ( 1164.618, 1176.358)

Report for Number of served contacts
            num obs.     min        max        average     std. dev.   conf. int.
Inbound     1000      425.000    1434.000      843.477     149.608   95.0% (  834.193,  852.761)
Outbound    1000       19.000     444.000      298.876      68.318   95.0% (  294.637,  303.115)
All types   1000      825.000    1465.000     1142.353      93.386   95.0% ( 1136.558, 1148.148)

Report for Number of abandoned contacts
            num obs.     min        max        average     std. dev.   conf. int.
Inbound     1000        0.000     130.000        5.115      9.985   95.0% (    4.495,    5.735)
Outbound    1000        0.000      49.000       23.020      7.609   95.0% (   22.548,   23.492)
All types   1000        8.000     132.000       28.135      8.843   95.0% (   27.586,   28.684)

REPORT on Tally stat. collector ==> Number of contacts not in target
    num. obs.      min          max        average     standard dev.
      1000        0.000      749.000       39.427       63.682

REPORT on Tally stat. collector ==> Service level
    func. of averages    standard dev.  num. obs.
              95.354        6.894         1000

Report for Agents' occupancy ratio
                func. of averages     std. dev.   nobs.   conf. int.
Inbound only                81.596       7.447    1000   95.0% (   81.135,   82.058)
Blend                       55.665       6.900    1000   95.0% (   55.237,   56.092)
All groups                  70.514       6.818    1000   95.0% (   70.091,   70.936)

Report for Time-average queue size
            num obs.     min        max       average     std. dev.   conf. int.
Inbound     1000        0.000      2.967       0.115      0.234   95.0% (    0.101,    0.130)
Outbound    1000        0.000      0.012       5.8E-3    1.9E-3   95.0% (   5.7E-3,   5.9E-3)
All types   1000       4.8E-3      2.967       0.121      0.233   95.0% (    0.107,    0.135)

Report for Waiting time
             func. of averages     std. dev.   nobs.   conf. int.
Inbound                 0.049       0.094      1000   95.0% (    0.043,    0.055)
Outbound               6.5E-3      1.2E-3      1000   95.0% (   6.4E-3,   6.6E-3)
All types               0.037       0.070      1000   95.0% (    0.033,    0.042)

Report for Number of contacts having to wait
            num obs.     min        max        average     std. dev.   conf. int.
Inbound     1000        0.000     913.000       70.241      86.800   95.0% (   64.855,   75.627)
Outbound    1000        0.000      57.000       27.445       9.034   95.0% (   26.884,   28.006)
All types   1000       23.000     921.000       97.686      81.085   95.0% (   92.654,  102.718)
