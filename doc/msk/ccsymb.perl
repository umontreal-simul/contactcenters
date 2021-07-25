&do_require_package ('amsfonts');

sub do_math_cmd_Ki {
    ("<I>K</I><SUB>I</SUB>", @_);
}

sub do_math_cmd_Ko {
    ("<I>K</I><SUB>O</SUB>", @_);
}

sub do_math_cmd_RR {
    ("<B>R</B>", @_);
}

sub do_math_cmd_NN {
    ("<B>N</B>", @_);
}

sub do_math_cmd_boldtheta {
    ("<I><B>&theta;</B></I>", @_);
}

sub do_math_cmd_boldmu {
    ("<I><B>&mu;</B></I>", @_);
}

sub do_math_cmd_boldnu {
    ("<I><B>&nu;</B></I>", @_);
}

sub do_math_cmd_boldbeta {
    ("<I><B>&beta;</B></I>", @_);
}

sub do_math_cmd_barboldmu {
    ("bar(<I><B>&mu;</B></I>)", @_);
}

sub do_math_cmd_boldSigma {
    ("<I><B>&Sigma;</B></I>", @_);
}

sub do_math_cmd_boldC {
    ("<I><B>C</B></I>", @_);
}

sub do_math_cmd_boldS {
    ("<I><B>S</B></I>", @_);
}

sub do_math_cmd_boldX {
    ("<I><B>X</B></I>", @_);
}

sub do_math_cmd_barboldX {
    ("bar(<I><B>X</B></I>)", @_);
}

sub do_math_cmd_boldP {
    ("<I><B>P</B></I>", @_);
}

sub do_math_cmd_boldY {
    ("<I><B>Y</B></I>", @_);
}

sub do_math_cmd_boldV {
    ("<B><I>V</I></B>", @_);
}

sub do_math_cmd_boldf {
    ("<I><B>f</B></I>", @_);
}

sub do_math_cmd_rTG {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>r</I><SUB>TG" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_rGT {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>r</I><SUB>GT" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_iTG {
    ("<I>i</I><SUB>TG</SUB>", @_);
}

sub do_math_cmd_iGT {
    ("<I>i</I><SUB>GT</SUB>", @_);
}

sub do_math_cmd_Nf {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>N</I><SUB>F" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_Nb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>N</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_Ni {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>N</I><SUB>F" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_Ng {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>N</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_Ntb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>N</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB><SUP>T</SUP>", $_);
}

sub do_math_cmd_Ntf {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>N</I><SUB>F" . ($n ? "<I>, $n</I>" : "") . "</SUB><SUP>T</SUP>", $_);
}

sub do_math_cmd_Ndf {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>N</I><SUB>F" . ($n ? "<I>, $n</I>" : "") . "</SUB><SUP>D</SUP>", $_);
}

sub do_math_cmd_Xb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>X</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barXb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>X</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_XbK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>X</I><SUB>B<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_XbP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>X</I><SUB>B<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Xg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>X</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barXg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>X</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_XgK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>X</I><SUB>G<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_XgP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>X</I><SUB>G<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Yb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>Y</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barYb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>Y</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_YbK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>Y</I><SUB>B<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_YbP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>Y</I><SUB>B<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Yg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>Y</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barYg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>Y</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_YgK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>Y</I><SUB>G<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_YgP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>Y</I><SUB>G<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}





sub do_math_cmd_Sb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>S</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barSb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>S</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_SbK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>S</I><SUB>B<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_SbP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>S</I><SUB>B<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Sg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>S</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barSg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>S</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_SgK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>S</I><SUB>G<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_SgP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>S</I><SUB>G<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Lb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>L</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barLb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>L</I><SUB>B" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_LbK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>L</I><SUB>B<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_LbP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>L</I><SUB>B<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Lg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>L</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_barLg {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("bar(<I>L</I><SUB>G" . ($n ? "<I>, $n</I>" : "") . "</SUB>)", $_);
}

sub do_math_cmd_LgK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>L</I><SUB>G<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_LgP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>L</I><SUB>G<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Prob {
    ("P", @_);
}

sub do_math_cmd_I {
    ("I", @_);
}

sub do_math_cmd_E {
    ("E", @_);
}

sub do_math_cmd_Var {
    ("Var", @_);
}

sub do_math_cmd_Cov {
    ("Cov", @_);
}

sub do_math_cmd_sK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>s</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_sP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>s</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_lK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>l</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_lP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_next_token;
    ("<I>l</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_XK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>X</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_XP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>X</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_YK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>Y</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_YP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>Y</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_SK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>S</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_SP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>S</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_LK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>L</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_LP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>L</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_BK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>B</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_BP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>B</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_AK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>A</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_AP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>A</I><SUB><I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_WK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>W</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_WP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>W</I><SUB><I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_WS {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>W</I><SUB>S" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_WSK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>W</I><SUB>S<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_WSP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>W</I><SUB>S<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_WL {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>W</I><SUB>L" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_WLK {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>W</I><SUB>L<I>, $n, ." . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_WLP {
    local($_) = @_;
    local($m) = &get_next_optional_argument;
    local($n) = &get_token;
    ("<I>W</I><SUB>L<I>, ., $n" . ($m ? ", $m" : "") . "</I></SUB>", $_);
}

sub do_math_cmd_Ntb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>X</I><SUB>C" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_Ntb {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I><B>X</B></I><SUB>C" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_XC {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>X</I><SUB>C" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_boldXC {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I><B>X</B></I><SUB>C" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_rmc {
    ("c", @_);
}

sub do_math_cmd_rmC {
    ("C", @_);
}

sub do_math_cmd_rmCX {
    ("CX", @_);
}

sub do_math_cmd_rmX {
    ("X", @_);
}

sub do_math_cmd_tr {
    ("t", @_);
}

sub do_math_cmd_wTG {
    ("<I>w</I><SUB>TG</SUB>", @_);
}

sub do_math_cmd_wGT {
    ("<I>w</I><SUB>GT</SUB>", @_);
}

sub do_math_cmd_dII {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>D</I><SUB>II" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_dIO {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>D</I><SUB>IO" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_dOI {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>D</I><SUB>OI" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_dOO {
    local($_) = @_;
    local($n) = &get_next_optional_argument;
    ("<I>D</I><SUB>OO" . ($n ? "<I>, $n</I>" : "") . "</SUB>", $_);
}

sub do_math_cmd_pawt {
    ("<I>P</I><SUB>AWT</SUB>", @_);
}

sub do_math_cmd_pstat {
    ("<I>P</I><SUB>STAT</SUB>", @_);
}

1;
