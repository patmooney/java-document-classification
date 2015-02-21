package Scraper::Monster;
use strict; use warnings;

use base q{Scraper::Base};

use Carp;
use Data::Dumper;
use Advert;

sub categories { 
    my ( $self ) = shift;
    my $dom = $self->get_dom( 'http://jobsearch.monster.co.uk/StandardAdvancedSearch.aspx?cy=uk&xmso=1' );
    my @group_elements = $dom->find('select[name="ctl00$ctl00$ctl00$body$body$wacCenterStage$ddlCategory1"] > optgroup')->each;

    my @groups;
    foreach my $group ( @group_elements ) {
        my $group_name = $group->attr('label');
        my @options = $group->children('option')->each;

        map {
            my $label = join( ' > ', $group_name, $_->text );
            my $name = $label;
            $name =~ s/ > /__/g;
            $name =~ s/[^A-Za-z0-9_]/_/g;
            push @groups, {
                label => $label,
                name  => $name,
                value => $_->attr('value')
            };
        } @options;
    }

    return @groups;
}

sub scrape_page {
    my ( $self, $category, $page ) = @_;

    $self->mech->add_header( 'Accept' => 'text/html' );
    $self->mech->get( q{http://jobsearch.monster.co.uk/Search.aspx?cy=uk&occ=} . $category->{value} . q{&pg=} . $page );

    my $dom = $self->get_dom();
    my $result_table = $dom->find('table.listingsTable > tbody')->first;
    return unless ( $result_table );

    my @advert_rows = $result_table->children('tr')->each;

    my @adverts;
    foreach my $advert ( @advert_rows ) {
        next unless $advert->attr('class') =~ m/\A(?:odd|even)\z/i;
        my $title = $advert->find( '.jobTitleContainer > a' )->first;
        my $url = $title->attr('href');
        push @adverts, Advert->new({ title => $title->text, url => $url });
    }
    return @adverts;
}

sub advert_content {
    my ( $self, $advert ) = @_;
    my $dom = eval {
        $self->get_dom( $advert->url );
    };
    if ( $@ ){ warn $@; return; }

    my $content = $dom->find('div#jobBodyContent')->first() || return undef;
    return $content->all_text;
}

1;
